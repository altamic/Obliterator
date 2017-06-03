#include "nfcd.h"
#include "vendor/adbi/hook.h"
#include <dlfcn.h>
#include <unistd.h>
#include <stdio.h>

jboolean patchEnabled = false;
struct hook_t hook_config;
struct hook_t hook_rfcback;
//struct hook_t hook_send_data;
struct hook_t hook_stop_quick_timer;
struct hook_t hook_set_activated_tag_type;
struct hook_t hook_ce_api_cfg_isodep_tech;
struct hook_t hook_ce_activate_ntf;
//struct hook_t hook_StopRfDiscovery;
//struct hook_t hook_CeConfigureLocalTag;

static void onHostEmulationLoad(JNIEnv *jni, jclass _class, void *data);
static void hookNative();
const char *hooklibfile = "/system/lib/libnfc-nci.so";

static void onModuleLoad() __attribute__((constructor));

void onModuleLoad() {
    LOGI("onModuleLoad::begin");
    hookNative();
    LOGI("onModuleLoad::end");
}

/**
 * find a native symbol and hook it
 */
static void findAndHook(struct hook_t* eph, void* handle, const char *symbol, void* hookf, void **original) {
    *original = dlsym(handle, symbol);
    if(hook(eph, (unsigned int)*original, hookf) != -1) {
        LOGI("hooked: %s", symbol);
    }
}

/**
 * hook into native functions of the libnfc-nci broadcom nfc driver
 */
static void hookNative() {
    if (access(hooklibfile, F_OK) == -1) {
        LOGE("could not access %s to load symbols", hooklibfile);
        return;
    }
    void *handle = dlopen(hooklibfile, 0);

    findAndHook(&hook_config,  handle, "NFC_SetConfig",        (void*)&hook_NfcSetConfig, (void**)&nci_orig_NfcSetConfig);
    findAndHook(&hook_rfcback, handle, "NFC_SetStaticRfCback", (void*)&hook_SetRfCback,   (void**)&nci_orig_SetRfCback);

//    findAndHook(&hook_send_data, handle, "NFC_SendData", (void*)&nci_NFC_SendData,   (void**)&orig_NFC_SendData);
    findAndHook(&hook_stop_quick_timer, handle, "nfc_stop_quick_timer", (void*)&nci_nfc_stop_quick_timer,   (void**)&orig_nfc_stop_quick_timer);

    findAndHook(&hook_set_activated_tag_type, handle, "CE_SetActivatedTagType", (void*)&hook_ce_set_activated_tag_type,   (void**)&orig_ce_set_activated_tag_type);
    findAndHook(&hook_ce_api_cfg_isodep_tech, handle, "nfa_ce_api_cfg_isodep_tech", (void*)&hook_nfa_ce_api_cfg_isodep_tech,   (void**)&orig_ce_api_cfg_isodep_tech);
    findAndHook(&hook_ce_activate_ntf, handle, "nfa_ce_activate_ntf", (void*)&hook_nfa_ce_activate_ntf,   (void**)&orig_ce_activate_ntf);
//    findAndHook(&hook_StopRfDiscovery, handle, "NFA_StopRfDiscovery", (void*)&hook_NFA_StopRfDiscovery,   (void**)&orig_NFA_StopRfDiscovery);
//    findAndHook(&hook_CeConfigureLocalTag, handle, "NFA_CeConfigureLocalTag", (void*)&hook_NFA_CeConfigureLocalTag,   (void**)&orig_CeConfigureLocalTag);

    if (nci_orig_NfcSetConfig == hook_NfcSetConfig) LOGI("original missing");

    // find pointer to ce_t4t control structure
    ce_cb = (tCE_CB*)dlsym(handle, "ce_cb");

    // NFA_CE control structure
    nfa_ce_cb = (tNFA_CE_CB*)dlsym(handle, "nfa_ce_cb");

}

/**
 * simple logging function for byte buffers
 */
void loghex(const char *desc, const uint8_t *data, const int len) {
    int strlen = len * 3 + 1;
    char *msg = (char *) malloc((size_t) strlen);
    for (uint8_t i = 0; i < len; i++) {
        sprintf(msg + i * 3, " %02x", (unsigned int) *(data + i));
    }
    LOGI("%s%s",desc, msg);
    free(msg);
}
