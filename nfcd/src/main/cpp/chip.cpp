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

static void ce_t2t_data_cback (UINT8, tNFC_CONN_EVT, tNFC_CONN *);

NFC_SetStaticRfCback *nci_orig_SetRfCback;
NFC_SetConfig *nci_orig_NfcSetConfig;
tCE_CB *ce_cb;
tNFA_CE_CB *nfa_ce_cb;

//tNFC_SendData *orig_NFC_SendData;
tnfc_stop_quick_timer *orig_nfc_stop_quick_timer;

tCE_SetActivatedTagType *orig_ce_set_activated_tag_type;
tNfa_ce_api_cfg_isodep_tech *orig_ce_api_cfg_isodep_tech;
tnfa_ce_activate_ntf *orig_ce_activate_ntf;
//tNFA_StopRfDiscovery *orig_NFA_StopRfDiscovery;
//tNFA_CeConfigureLocalTag *orig_CeConfigureLocalTag;

void nci_SetRfCback(tNFC_CONN_CBACK *p_cback) {
    hook_precall(&hook_rfcback);
    nci_orig_SetRfCback(p_cback);
    hook_postcall(&hook_rfcback);
}

tNFC_STATUS nci_NfcSetConfig (uint8_t size, uint8_t *tlv) {
    hook_precall(&hook_config);
    tNFC_STATUS r = nci_orig_NfcSetConfig(size, tlv);
    hook_postcall(&hook_config);
    return r;
}

//tNFC_STATUS nci_NFC_SendData(UINT8 conn_id, BT_HDR *p_data) {
//    hook_precall(&hook_send_data);
//    tNFC_STATUS r = orig_NFC_SendData(conn_id, p_data);
//    hook_postcall(&hook_send_data);
//    return r;
//}

void nci_nfc_stop_quick_timer(TIMER_LIST_ENT *p_tle) {
    hook_precall(&hook_stop_quick_timer);
    orig_nfc_stop_quick_timer(p_tle);
    hook_postcall(&hook_stop_quick_timer);
}

tNFC_STATUS nci_ce_set_activated_tag_type(tNFC_ACTIVATE_DEVT *p_activate_params,
                                           UINT16 t3t_system_code, tCE_CBACK *p_cback) {
    hook_precall(&hook_set_activated_tag_type);
    tNFC_STATUS status = orig_ce_set_activated_tag_type(p_activate_params,
                                                   t3t_system_code, p_cback);
    hook_postcall(&hook_set_activated_tag_type);
    return status;
}

BOOLEAN nci_nfa_ce_api_cfg_isodep_tech(tNFA_CE_MSG *p_ce_msg) {
    hook_precall(&hook_ce_api_cfg_isodep_tech);
    BOOLEAN result = orig_ce_api_cfg_isodep_tech(p_ce_msg);
    hook_postcall(&hook_ce_api_cfg_isodep_tech);
    return result;
}

BOOLEAN nci_nfa_ce_activate_ntf(tNFA_CE_MSG *p_ce_msg) {
    hook_precall(&hook_ce_activate_ntf);
    BOOLEAN result = orig_ce_activate_ntf(p_ce_msg);
    hook_postcall(&hook_ce_activate_ntf);
    return result;
}

//tNFA_STATUS NFA_StopRfDiscovery (void) {
//    hook_precall(&hook_StopRfDiscovery);
//    tNFA_STATUS  result = orig_NFA_StopRfDiscovery();
//    hook_postcall(&hook_StopRfDiscovery);
//    return result;
//}

//tNFA_STATUS NFA_CeConfigureLocalTag (tNFA_PROTOCOL_MASK protocol_mask,
//                                     UINT8     *p_ndef_data,
//                                     UINT16    ndef_cur_size,
//                                     UINT16    ndef_max_size,
//                                     BOOLEAN   read_only,
//                                     UINT8     uid_len,
//                                     UINT8     *p_uid) {
//    hook_precall(&hook_CeConfigureLocalTag);
//    tNFA_STATUS result = orig_CeConfigureLocalTag(protocol_mask,
//                                                  p_ndef_data,
//                                                  ndef_cur_size,
//                                                  ndef_max_size,
//                                                  read_only,
//                                                  uid_len,
//                                                  p_uid);
//    hook_postcall(&hook_CeConfigureLocalTag);
//}

tNFC_STATUS ce_select_t2t (void);

tNFC_STATUS hook_ce_set_activated_tag_type(tNFC_ACTIVATE_DEVT *p_activate_params,
                                          UINT16 t3t_system_code, tCE_CBACK *p_cback) {
    tNFC_STATUS status;

    if (patchEnabled) {
        LOGD("patchEnabled hook_CE_SetActivatedTagType");
        // activate ce for t2t
        tNFC_PROTOCOL protocol = p_activate_params->protocol;
        if (protocol == NFC_PROTOCOL_T2T) {
            /* store callback function before NFC_SetStaticRfCback () */
            ce_cb->p_cback = p_cback; // should be used?
            status = ce_select_t2t();
        }
        LOGD("patchEnabled hook_CE_SetActivatedTagType tNFC_STATUS: 0x%02x", status);
    } else {
        status = nci_ce_set_activated_tag_type(p_activate_params,
                                      t3t_system_code, p_cback);
    }
    return status;
}

BOOLEAN hook_nfa_ce_api_cfg_isodep_tech(tNFA_CE_MSG *p_ce_msg) {
    LOGD("hook_nfa_ce_api_cfg_isodep_tech");
    BOOLEAN result = nci_nfa_ce_api_cfg_isodep_tech(p_ce_msg);

    if (patchEnabled) {
        nfa_ce_cb->isodep_disc_mask  = 0; //???
        if (p_ce_msg->hdr.layer_specific & NFA_TECHNOLOGY_MASK_A) {
            nfa_ce_cb->isodep_disc_mask = NFA_DM_DISC_MASK_LA_ISO_DEP;
            nfa_ce_cb->isodep_disc_mask |= NFA_DM_DISC_MASK_LA_T2T;
        }

        if (p_ce_msg->hdr.layer_specific & NFA_TECHNOLOGY_MASK_B)
            nfa_ce_cb->isodep_disc_mask |= NFA_DM_DISC_MASK_LB_ISO_DEP;

        p_ce_msg->activate_ntf.hdr.event = NFA_CE_ACTIVATE_NTF_EVT;

        result = TRUE;
    }
//    else {
//       result = nci_nfa_ce_api_cfg_isodep_tech(p_ce_msg);
//    }

    return result;
}

BOOLEAN hook_nfa_ce_activate_ntf(tNFA_CE_MSG *p_ce_msg) {
    BOOLEAN result;
//    BOOLEAN result = nci_nfa_ce_activate_ntf(p_ce_msg);

    if (patchEnabled) {
        LOGD("patchEnabled nfa_ce_activate_ntf");
        tNFA_CE_CB *p_cb = nfa_ce_cb;
        tCE_CBACK *p_ce_cback = NULL;
        UINT8 listen_info_idx = NFA_CE_LISTEN_INFO_IDX_INVALID;
        UINT8 i;
        BOOLEAN t4t_activate_pending = FALSE;

        if (p_cb->activation_params.protocol == NFA_PROTOCOL_T2T) {
            /* For all T2T entries in listen_info, set T4T_ACTIVATE_NOTIFY_PENDING flag */
            for (i = 0; i < NFA_CE_LISTEN_INFO_IDX_INVALID; i++) {
                if (p_cb->listen_info[i].flags & NFA_CE_LISTEN_INFO_IN_USE) {
                    if ((p_cb->listen_info[i].protocol_mask & NFA_PROTOCOL_MASK_T2T) ||
                        (p_cb->listen_info[i].protocol_mask & NFA_PROTOCOL_MASK_ISO_DEP)) {
                        /* Found listen_info table entry for T2T raw listen */
                        p_cb->listen_info[i].flags |= NFA_CE_LISTEN_INFO_T4T_ACTIVATE_PND;

                        t4t_activate_pending = TRUE;
                    }
                }
            }
            /* If listening for T2T, then notify CE module now and */
            if (t4t_activate_pending && (listen_info_idx == NFA_CE_LISTEN_INFO_IDX_INVALID)) {
                hook_ce_set_activated_tag_type(&p_cb->activation_params, 0, p_ce_cback);
            }

            result = TRUE;

            // the following instead will not notificate the upper layer of a pending activation
//        if (p_cb->activation_params.protocol == NFA_PROTOCOL_T2T) {
//            /* For all T2T entries in listen_info, set NFA_ACTIVATED_EVT flag */
//            for (i = 0; i < NFA_CE_LISTEN_INFO_IDX_INVALID; i++) {
//                if (p_cb->listen_info[i].flags & NFA_CE_LISTEN_INFO_IN_USE) {
//                    if ((p_cb->listen_info[i].protocol_mask & NFA_PROTOCOL_MASK_T2T) ||
//                        (p_cb->listen_info[i].protocol_mask & NFA_PROTOCOL_MASK_ISO_DEP)) {
//                        /* Found listen_info table entry for T2T raw listen */
//                        p_cb->listen_info[i].flags &= ~(NFA_CE_LISTEN_INFO_T4T_ACTIVATE_PND);
//                        p_cb->listen_info[i].flags |= NFA_ACTIVATED_EVT;
//
//                        p_ce_msg->activate_ntf.hdr.event = NFA_CE_ACTIVATE_NTF_EVT;
//                    }
//                }
//            }
//
//            /* If listening for T2T, then notify CE module now and */
//            hook_ce_set_activated_tag_type(&p_cb->activation_params, 0, p_ce_cback);
//            result = TRUE;
//        }
        } else {
            result = nci_nfa_ce_activate_ntf(p_ce_msg);
        }

        return result;
    }
}

//tNFA_STATUS hook_NFA_StopRfDiscovery (void) {
//
// // tNFA_STATUS NFA_StopRfDiscovery (void)
// // {
// //     BT_HDR *p_msg;
// //
// //     NFA_TRACE_API0 ("NFA_StopRfDiscovery ()");
// //
// //     if ((p_msg = (BT_HDR *) GKI_getbuf (sizeof (BT_HDR))) != NULL)
// //     {
// //         p_msg->event = NFA_DM_API_STOP_RF_DISCOVERY_EVT; // stop RF discovery and then configure the local tag
// //
// //         nfa_sys_sendmsg (p_msg);
// //
// //         return (NFA_STATUS_OK);
// //     }
// //
// //     return (NFA_STATUS_FAILED);
// // }
//    tNFA_STATUS result = NFA_StopRfDiscovery();
//    return result;
//}

//tNFA_STATUS hook_NFA_CeConfigureLocalTag(tNFA_PROTOCOL_MASK protocol_mask,
//                                             UINT8 *p_ndef_data,
//                                             UINT16 ndef_cur_size,
//                                             UINT16 ndef_max_size,
//                                             BOOLEAN read_only,
//                                             UINT8 uid_len,
//                                             UINT8 *p_uid) {
//
//        tNFA_STATUS result = NFA_CeConfigureLocalTag(protocol_mask,
//                                                     p_ndef_data,
//                                                     ndef_cur_size,
//                                                     ndef_max_size,
//                                                     read_only,
//                                                     uid_len,
//                                                     p_uid);
//
//        if (patchEnabled) {
//            tNFA_CE_MSG *p_msg; // si
//
//            LOGD ("NFA_CeConfigureLocalTag ()");
//
//            if (protocol_mask) {
//                /* If any protocols are specified, then NDEF buffer pointer must be non-NULL */
//                if (p_ndef_data == NULL) {
//                    LOGD ("NFA_CeConfigureLocalTag: NULL ndef data pointer");
//                    result = (NFA_STATUS_INVALID_PARAM);
//                } // skip
//
//                if ((protocol_mask & NFA_PROTOCOL_MASK_T1T) ||
//                    (protocol_mask & NFA_PROTOCOL_MASK_T2T)) {
//                    LOGD ("NFA_CeConfigureLocalTag: Cannot emulate Type 1 / Type 2 tag");
//                    result = (NFA_STATUS_INVALID_PARAM);
//                } // skip: protocol_mask = NFA_PROTOCOL_MASK_T2T | NFA_PROTOCOL_MASK_ISO_DEP
//
//                if (uid_len) {
//                    LOGD ("NFA_CeConfigureLocalTag: Cannot Set UID for Protocol_mask: 0x%x",
//                          protocol_mask);
//                    result = (NFA_STATUS_INVALID_PARAM);
//                } // skip uid_len = 7
//            }
//            if ((p_msg = (tNFA_CE_MSG *) GKI_getbuf((UINT16) sizeof(tNFA_CE_MSG))) != NULL) {
//                p_msg->local_tag.hdr.event = NFA_CE_API_CFG_LOCAL_TAG_EVT;
//
//                /* Copy ndef info */
//                p_msg->local_tag.protocol_mask = protocol_mask;
//                p_msg->local_tag.p_ndef_data = p_ndef_data;
//                p_msg->local_tag.ndef_cur_size = ndef_cur_size;
//                p_msg->local_tag.ndef_max_size = ndef_max_size;
//                p_msg->local_tag.read_only = read_only;
//                p_msg->local_tag.uid_len = uid_len;
//
//                if (uid_len)
//                    memcpy(p_msg->local_tag.uid, p_uid, uid_len);
//
//                nfa_sys_sendmsg(p_msg); // hook nfa_sys_sendmsg
//
//                return (NFA_STATUS_OK);
//            }
//
//            return (NFA_STATUS_FAILED);
//        }
//    }


/**
 * hooked SetRfCback implementation.
 * call the original function, but modify the control structure if the patch is enabled
 */
void hook_SetRfCback(tNFC_CONN_CBACK *p_cback) {
    if (patchEnabled) {
        // fake that the default aid is selected
        ce_cb->mem.t4t.status &= ~(CE_T4T_STATUS_CC_FILE_SELECTED);
        ce_cb->mem.t4t.status &= ~(CE_T4T_STATUS_NDEF_SELECTED);
        ce_cb->mem.t4t.status &= ~(CE_T4T_STATUS_T4T_APP_SELECTED);
        ce_cb->mem.t4t.status &= ~(CE_T4T_STATUS_REG_AID_SELECTED);
        ce_cb->mem.t4t.status |= CE_T4T_STATUS_WILDCARD_AID_SELECTED; // or CE_T4T_STATUS_T4T_APP_SELECTED

        if (p_cback != NULL) {
            LOGD("invoke hook_nci_SetRfCback with not null ce_t2t_data_cback");
            nci_SetRfCback(ce_t2t_data_cback);
        }
    } else {
        LOGD("original nci_SetRfCback with not NULL p_cback");
        nci_SetRfCback(p_cback);
    }
}

/**
 * hooked NfcSetConfig implementation
 */
tNFC_STATUS hook_NfcSetConfig (uint8_t size, uint8_t *tlv) {

    loghex("NfcSetConfig", tlv, size);
    uint8_t i = 0;
    bool needUpload = false;
    // read the configuration bytestream and extract the values that we intend to override
    // if we are in an active mode and the value gets overridden, then upload our configuration afterwards
    // in any case: save the values to allow re-uploading when the patch is deactivated
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
        }
    }

    tNFC_STATUS r = nci_NfcSetConfig(size, tlv);

    if(needUpload && patchEnabled) {
        // any of our values got modified and we are active -> re-upload
        uploadPatchConfig();
//                hook_NFA_CeConfigureLocalTag();

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


/*******************************************************************************
**
** Function         ce_t2t_send_to_lower
**
** Description      Send packet to lower layer
**
** Returns          TRUE if success
**
*******************************************************************************/
//static BOOLEAN ce_t2t_send_to_lower (BT_HDR *p_r_apdu)
//{
//
//    if (nci_NFC_SendData (NFC_RF_CONN_ID, p_r_apdu) != NFC_STATUS_OK)
//    {
//        LOGE ("ce_t2t_send_to_lower (): NFC_SendData () failed");
//        return FALSE;
//    }
//    return TRUE;
//}

/*******************************************************************************
**
** Function         ce_t2t_process_timeout
**
** Description      process timeout event
**
** Returns          none
**
*******************************************************************************/
void ce_t2t_process_timeout (TIMER_LIST_ENT *p_tle)
{
    LOGD("ce_t2t_process_timeout () event=%d", p_tle->event);
}

/*******************************************************************************
**
** Function         ce_t2t_data_cback
**
** Description      This callback function receives the data from NFCC.
**
** Returns          none
**
*******************************************************************************/
static void ce_t2t_data_cback (UINT8 conn_id, tNFC_CONN_EVT event, tNFC_CONN *p_data)
{
    BT_HDR  *p_c_apdu;
    UINT8   *p_cmd;
    tCE_DATA ce_data;

    LOGD ("ce_t2t_data_cback (): event=0x%02X", event);

    if (event == NFC_DEACTIVATE_CEVT)
    {
        nci_orig_SetRfCback (NULL);
        return;
    }

    if (event != NFC_DATA_CEVT)
    {
        return;
    }

    p_c_apdu = (BT_HDR *) p_data->data.p_data;

    LOGD ("ce_t2t_data_cback (): conn_id=0x%02X", conn_id);

    p_cmd = (UINT8 *) (p_c_apdu + 1) + p_c_apdu->offset;

    /* forward raw frame to upper layer */
    ce_data.raw_frame.status = p_data->data.status;
    ce_data.raw_frame.p_data = p_c_apdu;
    ce_data.raw_frame.aid_handle = CE_T4T_WILDCARD_AID_HANDLE;

    LOGD ("CET2T: Forward raw frame to wildcard AID handler");
    (*(ce_cb->mem.t4t.p_wildcard_aid_cback)) (CE_T4T_RAW_FRAME_EVT, &ce_data);

}

/*******************************************************************************
**
** Function         ce_select_t2t
**
** Description      Select Type 2 Tag
**
** Returns          NFC_STATUS_OK if success
**
*******************************************************************************/
tNFC_STATUS ce_select_t2t (void)
{
    tCE_T4T_MEM *p_t4t = &ce_cb->mem.t4t;

    LOGD ("ce_select_t2t ()");

    nci_nfc_stop_quick_timer (&p_t4t->timer);

    p_t4t->status = CE_T4T_STATUS_WILDCARD_AID_SELECTED; // or CE_T4T_STATUS_T4T_APP_SELECTED

    hook_SetRfCback (ce_t2t_data_cback);

    return NFC_STATUS_OK;
}
