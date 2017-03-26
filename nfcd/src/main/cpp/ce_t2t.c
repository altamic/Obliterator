/******************************************************************************
 *
 *  This file contains the implementation for Type 2 tag in Card Emulation
 *  mode.
 *
 ******************************************************************************/

#include "nfcd.h"

/*******************************************************************************
**
** Function         ce_t2t_send_to_lower
**
** Description      Send packet to lower layer
**
** Returns          TRUE if success
**
*******************************************************************************/
static BOOLEAN ce_t2t_send_to_lower (BT_HDR *p_r_apdu)
{
//#if (BT_TRACE_PROTOCOL == TRUE)
//    DispCET4Tags (p_r_apdu, FALSE);
//#endif

    if (nci_NFC_SendData (NFC_RF_CONN_ID, p_r_apdu) != NFC_STATUS_OK)
    {
//        CE_TRACE_ERROR0 ("ce_t2t_send_to_lower (): NFC_SendData () failed");
        return FALSE;
    }
    return TRUE;
}

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
//    CE_TRACE_DEBUG1 ("ce_t2t_process_timeout () event=%d", p_tle->event);
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

//#if (BT_TRACE_PROTOCOL == TRUE)
//    DispCET4Tags (p_c_apdu, TRUE);
//#endif

//    CE_TRACE_DEBUG1 ("ce_t2t_data_cback (): conn_id = 0x%02X", conn_id);

    p_cmd = (UINT8 *) (p_c_apdu + 1) + p_c_apdu->offset;

//    CE_TRACE_DEBUG0 ("CET2T: Forward raw frame to wildcard AID handler");

    /* forward raw frame to upper layer */
    ce_data.raw_frame.status = p_data->data.status;
    ce_data.raw_frame.p_data = p_c_apdu;
    ce_data.raw_frame.aid_handle = CE_T4T_WILDCARD_AID_HANDLE;
    p_c_apdu = NULL;

    (*(ce_cb->mem.t4t.p_wildcard_aid_cback)) (CE_T4T_RAW_FRAME_EVT, &ce_data);

//    if (p_c_apdu)
//        GKI_freebuf (p_c_apdu);
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

//    CE_TRACE_DEBUG0 ("ce_select_t2t ()");

    orig_nfc_stop_quick_timer (&p_t4t->timer);

    p_t4t->status = 0;

    nci_orig_SetRfCback (ce_t2t_data_cback);

    return NFC_STATUS_OK;
}
