

#include "nfcd.h"
#include "vendor/adbi/hook.h"
#include <cstring>
/**
 * Commands of the broadcom configuration interface
 */
#define CFG_TYPE_ATQA  0x31
#define CFG_TYPE_SAK   0x32
#define CFG_TYPE_UID   0x33
#define CFG_TYPE_HIST  0x59

static void uploadConfig(const s_chip_config config);

struct s_chip_config origValues = { 0 };
struct s_chip_config patchValues = { 0 };

NFC_SetStaticRfCback *nci_orig_SetRfCback;
NFC_SetConfig *nci_orig_NfcSetConfig;
NFA_CeConfigureLocalTag *nci_orig_NfaCeConfigureLocalTag;

tCE_CB *ce_cb;

void nci_SetRfCback(tNFC_CONN_CBACK *p_cback) {
    hook_precall(&hook_rfcback);
    nci_orig_SetRfCback(p_cback);
    hook_postcall(&hook_rfcback);
}

tNFC_STATUS nci_NfcSetConfig(uint8_t size, uint8_t *tlv) {
    hook_precall(&hook_config);
    tNFC_STATUS r = nci_orig_NfcSetConfig(size, tlv);
    hook_postcall(&hook_config);
    return r;
}

tNFA_STATUS nci_ceConfigureLocalTag(tNFA_PROTOCOL_MASK protocol_mask,
                                     UINT8     *p_ndef_data,
                                     UINT16    ndef_cur_size,
                                     UINT16    ndef_max_size,
                                     BOOLEAN   read_only,
                                     UINT8     uid_len,
                                     UINT8     *p_uid) {
    hook_precall(&hook_local_tag);
    tNFA_STATUS r = nci_orig_NfaCeConfigureLocalTag(protocol_mask, p_ndef_data,
                                 ndef_cur_size, ndef_max_size, read_only,
                                 uid_len, p_uid);
    hook_postcall(&hook_local_tag);
    return r;
}

/**
 * hooked SetRfCback implementation.
 * call the original function, but modify the control structure if the patch is enabled
 */
void hook_SetRfCback(tNFC_CONN_CBACK *p_cback) {
    LOGD("hook_SetRfCback");
    nci_SetRfCback(p_cback);
    if(p_cback != NULL && patchEnabled) {
        ce_cb->mem.t4t.status &= ~ (CE_T4T_STATUS_CC_FILE_SELECTED);
        ce_cb->mem.t4t.status &= ~ (CE_T4T_STATUS_WILDCARD_AID_SELECTED);
        ce_cb->mem.t4t.status &= ~ (CE_T4T_STATUS_T4T_APP_SELECTED);
        ce_cb->mem.t4t.status &= ~ (CE_T4T_STATUS_REG_AID_SELECTED);
        ce_cb->mem.t4t.status |= CE_T4T_STATUS_NDEF_SELECTED;
    }
}

/**
 * hooked NfcSetConfig implementation
 */
tNFC_STATUS hook_NfcSetConfig(uint8_t size, uint8_t *tlv) {

    loghex("NfcSetConfig", tlv, size);

    bool needUpload = false;
    // read the configuration bytestream and extract the values that we intend to override
    // if we are in an active mode and the value gets overridden, then upload our configuration afterwards
    // in any case: save the values to allow re-uploading when deactivation the patch
    uint8_t i = 0;
    while (size > i + 2) {
        // first byte: type
        // second byte: len (if len = 0, then val = 0)
        // following bytes: value (length: len)
        uint8_t type = *(tlv + i);
        uint8_t len  = *(tlv + i + 1);
        uint8_t *valbp = tlv + i + 2;
        uint8_t firstval = len ? *valbp : 0;
        i += 2 + len;

        switch(type) {
            case CFG_TYPE_ATQA:
                needUpload = true;
                origValues.atqa = firstval;
                LOGD("NfcSetConfig Read: ATQA 0x%02x", firstval);
                break;

            case CFG_TYPE_SAK:
                needUpload = true;
                origValues.sak = firstval;
                LOGD("NfcSetConfig Read: SAK  0x%02x", firstval);
                break;

            case CFG_TYPE_HIST:
                needUpload = true;
                if(len > sizeof(origValues.hist)) {
                    LOGE("cannot handle an hist with len=0x%02x", len);
                } else {
                    memcpy(origValues.hist, valbp, len);
                    origValues.uid_len = len;
                    loghex("NfcSetConfig Read: HIST", valbp, len);
                }
                break;

            case CFG_TYPE_UID:
                needUpload = true;
                if(len > sizeof(origValues.uid)) {
                    LOGE("cannot handle an uid with len=0x%02x", len);
                } else {
                    memcpy(origValues.uid, valbp, len);
                    origValues.uid_len = len;
                    loghex("NfcSetConfig Read: UID", valbp, len);
                }
                break;

            default:
                break;
        }
    }

    tNFC_STATUS r = nci_NfcSetConfig(size, tlv);

    if(needUpload && patchEnabled) {
        // any of our values got modified and we are active -> re-upload
        uploadPatchConfig();
    }
    return r;
}

/**
 * write a single config value into a new configuration stream.
 * see uploadConfig()
 */
static void pushcfg(uint8_t *cfg, uint8_t &i, uint8_t type, uint8_t value) {
    cfg[i++] = type;
    if(value) {
      cfg[i++] = 1; // len
      cfg[i++] = value;
    } else {
      cfg[i++] = 0;
    }
}

/**
 * build a new configuration stream and upload it into the broadcom nfc controller
 */
static void uploadConfig(const struct s_chip_config config) {
    // cfg: type1, paramlen1, param1, type2, paramlen2....
    uint8_t cfg[80];
    uint8_t i=0;
    pushcfg(cfg, i, CFG_TYPE_SAK,  config.sak);
    //pushcfg(cfg, i, CFG_TYPE_HIST, config.hist);
    pushcfg(cfg, i, CFG_TYPE_ATQA, config.atqa);

    cfg[i++] = CFG_TYPE_UID;
    cfg[i++] = config.uid_len;

    memcpy(cfg+i, config.uid, config.uid_len);
    i += config.uid_len;

    cfg[i++] = CFG_TYPE_HIST;
    cfg[i++] = config.hist_len;
    memcpy(cfg+i, config.hist, config.hist_len);
    i += config.hist_len;

    nci_NfcSetConfig(i, cfg);
    loghex("Upload:", cfg, i);
}

/**
 * upload the values we got from the ipc
 */
void uploadPatchConfig() {
    uploadConfig(patchValues);
}

/**
 * upload the values we collected in  NfcSetConfig
 */
void uploadOriginalConfig() {
    uploadConfig(origValues);
}


static void uploadNdefConfig(const struct s_chip_config config) {
    loghex("ConfigureLocalTag:", config.data, config.data_len);

    tNFA_PROTOCOL_MASK protocol_mask = NFA_PROTOCOL_MASK_ISO_DEP;
    BOOLEAN readonly = FALSE;
    UINT8 * data_ptr = (UINT8 *) config.data;

    nci_ceConfigureLocalTag(protocol_mask, data_ptr,
                            config.data_len, config.data_len,
                            readonly, 0, NULL);
}

void uploadNdefData() {
    uploadNdefConfig(patchValues);
}

/**
 * hooked NfaCeConfigureLocalTag implementation
 */

tNFA_STATUS hook_CeConfigureLocalTag(tNFA_PROTOCOL_MASK protocol_mask,
                                     UINT8     *p_ndef_data,
                                     UINT16    ndef_cur_size,
                                     UINT16    ndef_max_size,
                                     BOOLEAN   read_only,
                                     UINT8     uid_len,
                                     UINT8     *p_uid) {

    loghex("hook_NfaCeConfigureLocalTag", p_ndef_data, ndef_cur_size);

    tNFA_STATUS r = nci_orig_NfaCeConfigureLocalTag(protocol_mask, p_ndef_data, ndef_cur_size,
                                                 ndef_max_size, read_only, uid_len, p_uid);

    if (patchEnabled) {
        uploadNdefData();
    }

    return r;
}

