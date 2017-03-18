#ifndef __ANDROID__
#define __ANDROID__
#endif


#include <android/log.h>
#include <jni.h>
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
    uint8_t data[128];
    uint8_t data_len;
};

// main.cpp
extern jboolean patchEnabled;
void loghex(const char *desc, const uint8_t *data, const int len);

// chip.cpp
tNFC_STATUS hook_NfcSetConfig (UINT8 tlv_size, UINT8 *p_param_tlvs);
void hook_SetRfCback(tNFC_CONN_CBACK *p_cback);
void uploadPatchConfig();
void uploadOriginalConfig();

tNFC_STATUS hook_NFC_SendData(UINT8 conn_id, BT_HDR *p_data);
void hook_nfc_stop_quick_timer(TIMER_LIST_ENT *p_tle);

extern NFC_SetStaticRfCback *nci_orig_SetRfCback;
extern NFC_SetConfig *nci_orig_NfcSetConfig;

extern tCE_CB *ce_cb;

typedef tNFC_STATUS tNFC_SendData(UINT8, BT_HDR *);
typedef void tnfc_stop_quick_timer(TIMER_LIST_ENT *);

extern tNFC_SendData *orig_NFC_SendData;
extern tnfc_stop_quick_timer *orig_nfc_stop_quick_timer;


extern struct s_chip_config patchValues;
extern struct hook_t hook_config;
extern struct hook_t hook_rfcback;
extern struct hook_t hook_send_data;
extern struct hook_t hook_stop_quick_timer;




// ipc.cpp
void ipc_prepare();
void ipc_init();
