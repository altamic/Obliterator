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

//tNFC_STATUS nci_NFC_SendData(UINT8 conn_id, BT_HDR *p_data);
void nci_nfc_stop_quick_timer(TIMER_LIST_ENT *p_tle);

tNFC_STATUS hook_ce_set_activated_tag_type(tNFC_ACTIVATE_DEVT*, UINT16, tCE_CBACK*);
BOOLEAN hook_nfa_ce_api_cfg_isodep_tech(tNFA_CE_MSG *);
BOOLEAN hook_nfa_ce_activate_ntf(tNFA_CE_MSG *);

extern NFC_SetStaticRfCback *nci_orig_SetRfCback;
extern NFC_SetConfig *nci_orig_NfcSetConfig;

extern tCE_CB *ce_cb;
extern tNFA_CE_CB *nfa_ce_cb;

//typedef tNFC_STATUS tNFC_SendData(UINT8, BT_HDR *);
//extern tNFC_SendData *orig_NFC_SendData;

typedef void tnfc_stop_quick_timer(TIMER_LIST_ENT *);
extern tnfc_stop_quick_timer *orig_nfc_stop_quick_timer;

typedef tNFC_STATUS tCE_SetActivatedTagType(tNFC_ACTIVATE_DEVT *, UINT16, tCE_CBACK *);
extern tCE_SetActivatedTagType *orig_ce_set_activated_tag_type;

typedef BOOLEAN tNfa_ce_api_cfg_isodep_tech(tNFA_CE_MSG *);
extern tNfa_ce_api_cfg_isodep_tech *orig_ce_api_cfg_isodep_tech;

typedef BOOLEAN tnfa_ce_activate_ntf (tNFA_CE_MSG *);
extern tnfa_ce_activate_ntf *orig_ce_activate_ntf;

extern struct s_chip_config patchValues;
extern struct hook_t hook_config;
extern struct hook_t hook_rfcback;
//extern struct hook_t hook_send_data;
extern struct hook_t hook_stop_quick_timer;

extern struct hook_t hook_set_activated_tag_type;
extern struct hook_t hook_ce_api_cfg_isodep_tech;
extern struct hook_t hook_ce_activate_ntf;


// ipc.cpp
void ipc_prepare();
void ipc_init();
