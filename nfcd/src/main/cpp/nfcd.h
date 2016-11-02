#ifndef __ANDROID__
#define __ANDROID__
#endif


#include <android/log.h>
#include <jni.h>
#include <stdint.h>
#include "include/libnfc.h"

#define LOG_TAG "Obliterator"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__ )
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__ )
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * all values we override in one struct
 */
struct s_chip_config {
    uint8_t atqa;
    uint8_t sak;
    uint8_t hist[64];
    uint8_t hist_len;
    uint8_t uid[64];
    uint8_t uid_len;
    UINT8 data[128];
    uint8_t data_len;
};

// main.cpp
extern bool patchEnabled;
void loghex(const char *desc, const uint8_t *data, const int len);

// java.cpp
void hookJava(JNIEnv *jni, jclass _class);


// chip.cpp
tNFC_STATUS hook_NfcSetConfig(UINT8 tlv_size, UINT8 *p_param_tlvs);
void hook_SetRfCback(tNFC_CONN_CBACK *p_cback);
tNFA_STATUS hook_CeConfigureLocalTag(tNFA_PROTOCOL_MASK protocol_mask,
                                         UINT8 *p_ndef_data,
                                         UINT16 ndef_cur_size,
                                         UINT16 ndef_max_size,
                                         BOOLEAN read_only,
                                         UINT8 uid_len,
                                         UINT8 *p_uid);

void uploadPatchConfig();
void uploadOriginalConfig();

void uploadNdefData();


extern NFC_SetStaticRfCback *nci_orig_SetRfCback;
extern NFC_SetConfig *nci_orig_NfcSetConfig;
extern NFA_CeConfigureLocalTag *nci_orig_NfaCeConfigureLocalTag;

extern tCE_CB *ce_cb;

extern struct s_chip_config patchValues;
extern struct hook_t hook_rfcback;
extern struct hook_t hook_config;
extern struct hook_t hook_local_tag;


// ipc.cpp
void ipc_prepare();
void ipc_init();
