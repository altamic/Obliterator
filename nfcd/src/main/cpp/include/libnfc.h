/*
*****************************************************************
* data structures extracted from the original libnfc sourcecode *
*****************************************************************
*/

#include <stdint.h>

#define UINT16_TO_BE_STREAM(p, u16) {*(p)++ = (uint8_t)((u16) >> 8); *(p)++ = (uint8_t)(u16);}
#define UINT8_TO_BE_STREAM(p, u8)   {*(p)++ = (uint8_t)(u8);}
#define BE_STREAM_TO_UINT8(u8, p)   {u8 = (uint8_t)(*(p)); (p) += 1;}
#define BE_STREAM_TO_UINT16(u16, p) {u16 = (uint16_t)(((uint16_t)(*(p)) << 8) + (uint16_t)(*((p) + 1))); (p) += 2;}

#define NCI_STATUS_OK                   0x00
#define NFC_STATUS_OK                   NCI_STATUS_OK                   /* Command succeeded    */
#define NFA_STATUS_OK                   NCI_STATUS_OK                   /* Command succeeded    */

#define NFA_STATUS_FAILED               NCI_STATUS_FAILED               /* failed               */
#define NCI_STATUS_FAILED               0x03

#define NCI_STATUS_INVALID_PARAM        0x09
#define NFA_STATUS_INVALID_PARAM        NCI_STATUS_INVALID_PARAM        /* Invalid Parameter    */


#define NFA_DM_DISC_MASK_LA_T2T                 0x00020000

typedef unsigned char UINT8;
typedef unsigned short UINT16;
typedef unsigned char BOOLEAN;
typedef unsigned long UINT32;

#define FALSE  0
#define TRUE   (!FALSE)

#define NFC_RF_CONN_ID                 0    /* the static connection ID for RF traffic */

typedef void (tCE_CBACK)(uint8_t event, void *p_data);

typedef uint8_t CE_T4tRegisterAID(uint8_t aid_len, uint8_t *p_aid, tCE_CBACK *p_cback);

typedef struct {
    uint16_t event;
    uint16_t len;
    uint16_t offset;
    uint16_t layer_specific;
} BT_HDR;


typedef struct {
    uint8_t status;     /* The event status                 */
    BT_HDR *p_data;     /* The received Data                */
} tNFC_DATA_CEVT;

typedef union {
    tNFC_DATA_CEVT data;
} tNFC_CONN;

typedef struct {
    uint8_t status;
    uint8_t *p_data;
    uint8_t b_updated;
    uint32_t length;
} tCE_UPDATE_INFO;

typedef struct {
    uint8_t status;
    uint8_t aid_handle;
    BT_HDR *p_data;
} tCE_RAW_FRAME;

typedef union {
    uint8_t status;
    tCE_UPDATE_INFO update_info;
    tCE_RAW_FRAME raw_frame;
} tCE_DATA;

#define CE_T3T_FIRST_EVT    0x60
#define CE_T4T_FIRST_EVT    0x80

enum {
    CE_T3T_NDEF_UPDATE_START_EVT = CE_T3T_FIRST_EVT,
    CE_T3T_NDEF_UPDATE_CPLT_EVT,
    CE_T3T_UPDATE_EVT,
    CE_T3T_CHECK_EVT,
    CE_T3T_RAW_FRAME_EVT,
    CE_T3T_MAX_EVT,

    CE_T4T_NDEF_UPDATE_START_EVT = CE_T4T_FIRST_EVT,
    CE_T4T_NDEF_UPDATE_CPLT_EVT,
    CE_T4T_NDEF_UPDATE_ABORT_EVT,
    CE_T4T_RAW_FRAME_EVT,
    CE_T4T_MAX_EVT
};

#define NFC_FIRST_CEVT      0x6000

enum
{
    NFC_CONN_CREATE_CEVT = NFC_FIRST_CEVT,  /* 0  Conn Create Response          */
    NFC_CONN_CLOSE_CEVT,                    /* 1  Conn Close Response           */
    NFC_DEACTIVATE_CEVT,                    /* 2  Deactivate response/notificatn*/
    NFC_DATA_CEVT,                          /* 3  Data                          */
    NFC_ERROR_CEVT,                         /* 4  generic or interface error    */
    NFC_DATA_START_CEVT                     /* 5  received the first fragment on RF link */
};

#define CE_T4T_WILDCARD_AID_HANDLE  (CE_T4T_MAX_REG_AID)    /* reserved handle for wildcard aid */

typedef UINT16 tNFC_CONN_EVT;



/***************************************************/
typedef void (tNFC_CONN_CBACK)(uint8_t conn_id, uint16_t event, tNFC_CONN *p_data);

typedef void NFC_SetStaticRfCback(tNFC_CONN_CBACK *p_cback);


typedef BOOLEAN nfc_hal_nci_receive_msg(UINT8 byte);

typedef UINT8 tUSERIAL_PORT;

typedef UINT8 tNFC_DISCOVERY_TYPE;

#define NCI_NFCID1_MAX_LEN    10
#define NCI_T1T_HR_LEN        2

typedef struct {
    UINT8 sens_res[2];  /* SENS_RES Response (ATQA). Available after Technology Detection */
    UINT8 nfcid1_len;   /* 4, 7 or 10 */
    UINT8 nfcid1[NCI_NFCID1_MAX_LEN];   /* AKA NFCID1 */
    UINT8 sel_rsp;        /* SEL_RSP (SAK) Available after Collision Resolution */
    UINT8 hr_len;         /* 2, if T1T HR0/HR1 is reported */
    UINT8 hr[NCI_T1T_HR_LEN]; /* T1T HR0 is in hr[0], HR1 is in hr[1] */
} tNCI_RF_PA_PARAMS;

typedef tNCI_RF_PA_PARAMS tNFC_RF_PA_PARAMS;

#define NFC_MAX_SENSB_RES_LEN 12
#define NFC_NFCID0_MAX_LEN          4

typedef struct {
    UINT8 sensb_res_len;    /* Length of SENSB_RES Response (Byte 2 - Byte 12 or 13) Available after Technology Detection */
    UINT8 sensb_res[NFC_MAX_SENSB_RES_LEN]; /* SENSB_RES Response (ATQ) */
    UINT8 nfcid0[NFC_NFCID0_MAX_LEN];
} tNFC_RF_PB_PARAMS;

#define NFC_MAX_SENSF_RES_LEN 18
#define NFC_NFCID2_LEN 8
#define NCI_NFCID2_LEN 8

typedef struct {
    UINT8 bit_rate; /* NFC_BIT_RATE_212 or NFC_BIT_RATE_424 */
    UINT8 sensf_res_len;    /* Length of SENSF_RES Response (Byte 2 - Byte 17 or 19) Available after Technology Detection */
    UINT8 sensf_res[NFC_MAX_SENSF_RES_LEN]; /* SENSB_RES Response */
    UINT8 nfcid2[NFC_NFCID2_LEN];   /* NFCID2 generated by the Local NFCC for NFC-DEP Protocol.Available for Frame Interface  */
    UINT8 mrti_check;
    UINT8 mrti_update;
} tNFC_RF_PF_PARAMS;

typedef struct {
    UINT8 nfcid2[NCI_NFCID2_LEN];  /* NFCID2 generated by the Local NFCC for NFC-DEP Protocol.Available for Frame Interface  */
} tNCI_RF_LF_PARAMS;

typedef tNCI_RF_LF_PARAMS tNFC_RF_LF_PARAMS;

#define NFC_ISO15693_UID_LEN        8
typedef struct {
    UINT8 flag;
    UINT8 dsfid;
    UINT8 uid[NFC_ISO15693_UID_LEN];
} tNFC_RF_PISO15693_PARAMS;

#ifndef NFC_KOVIO_MAX_LEN
#define NFC_KOVIO_MAX_LEN       32
#endif
typedef struct {
    UINT8 uid_len;
    UINT8 uid[NFC_KOVIO_MAX_LEN];
} tNFC_RF_PKOVIO_PARAMS;

typedef union {
    tNFC_RF_PA_PARAMS pa;
    tNFC_RF_PB_PARAMS pb;
    tNFC_RF_PF_PARAMS pf;
    tNFC_RF_LF_PARAMS lf;
    tNFC_RF_PISO15693_PARAMS pi93;
    tNFC_RF_PKOVIO_PARAMS pk;
} tNFC_RF_TECH_PARAMU;

typedef struct {
    tNFC_DISCOVERY_TYPE mode;
    tNFC_RF_TECH_PARAMU param;
} tNFC_RF_TECH_PARAMS;

typedef void nfa_ce_init(void);

typedef UINT8 NFA_SetTraceLevel(UINT8 new_level);

typedef UINT8 tNFA_STATUS;
typedef UINT8 tNFC_STATUS;
typedef UINT8 tNFA_PROTOCOL_MASK;

typedef UINT32 tNFA_DM_DISC_TECH_PROTO_MASK;

typedef void HAL_NfcWrite(UINT16 data_len, UINT8 *p_data);

typedef UINT16 USERIAL_Write(tUSERIAL_PORT port, UINT8 *p_data, UINT16 len);

typedef tNFA_STATUS nfa_dm_set_rf_listen_mode_config(tNFA_DM_DISC_TECH_PROTO_MASK tech_proto_mask);

typedef tNFC_STATUS NFC_SetConfig(UINT8 tlv_size, UINT8 *p_param_tlvs);


/***************************************************/

#define T3T_MSG_SERVICE_LIST_MAX                16
//#define NCI_NFCID2_LEN                           8
#define NCI_T3T_PMM_LEN                          8
#define NCI_RF_F_UID_LEN            NCI_NFCID2_LEN
#define NCI_MAX_AID_LEN                         16
#define NFC_MAX_AID_LEN            NCI_MAX_AID_LEN     /* 16 */
#define CE_T4T_MAX_REG_AID                       4
#define T4T_FC_TLV_OFFSET_IN_CC               0x07
#define T4T_FILE_CONTROL_TLV_SIZE             0x08

typedef struct _tle {
    struct _tle *p_next;
    struct _tle *p_prev;
    void *p_cback;
    uint32_t ticks;
    uint32_t param;
    uint16_t event;
    uint8_t in_use;
} TIMER_LIST_ENT;

typedef struct {
    unsigned char initialized;
    uint8_t version;    /* Ver: peer version */
    uint8_t nbr;        /* NBr: number of blocks that can be read using one Check command */
    uint8_t nbw;        /* Nbw: number of blocks that can be written using one Update command */
    uint16_t nmaxb;     /* Nmaxb: maximum number of blocks available for NDEF data */
    uint8_t writef;     /* WriteFlag: 00h if writing data finished; 0Fh if writing data in progress */
    uint8_t rwflag;     /* RWFlag: 00h NDEF is read-only; 01h if read/write available */
    uint32_t ln;
    uint8_t *p_buf;     /* Current contents for READs */
    uint8_t scratch_writef;  /* Scratch NDEF buffer (for update NDEF commands) */
    uint32_t scratch_ln;
    uint8_t *p_scratch_buf; /* Scratch buffer for WRITE/readback */
} tCE_T3T_NDEF_INFO;

typedef struct {
    uint16_t service_code_list[T3T_MSG_SERVICE_LIST_MAX];
    uint8_t *p_block_list_start;
    uint8_t *p_block_data_start;
    uint8_t num_services;
    uint8_t num_blocks;
} tCE_T3T_CUR_CMD;

typedef struct {
    uint8_t state;
    uint16_t system_code;
    uint8_t local_nfcid2[NCI_RF_F_UID_LEN];
    uint8_t local_pmm[NCI_T3T_PMM_LEN];
    tCE_T3T_NDEF_INFO ndef_info;
    tCE_T3T_CUR_CMD cur_cmd;
} tCE_T3T_MEM;

typedef struct {
    uint8_t aid_len;
    uint8_t aid[NFC_MAX_AID_LEN];
    tCE_CBACK *p_cback;
} tCE_T4T_REG_AID;
/* registered AID table */

typedef struct {
    TIMER_LIST_ENT timer;
    /* timeout for update file              */
    uint8_t cc_file[T4T_FC_TLV_OFFSET_IN_CC + T4T_FILE_CONTROL_TLV_SIZE];
    uint8_t *p_ndef_msg;
    /* storage of NDEF message              */
    uint16_t nlen;
    /* current size of NDEF message         */
    uint16_t max_file_size;
    /* size of storage + 2 bytes for NLEN   */
    uint8_t *p_scratch_buf;      /* temp storage of NDEF message for update */

#define CE_T4T_STATUS_T4T_APP_SELECTED      0x01    /* T4T CE App is selected       */
#define CE_T4T_STATUS_REG_AID_SELECTED      0x02    /* Registered AID is selected   */
#define CE_T4T_STATUS_CC_FILE_SELECTED      0x04    /* CC file is selected          */
#define CE_T4T_STATUS_NDEF_SELECTED         0x08    /* NDEF file is selected        */
#define CE_T4T_STATUS_NDEF_FILE_READ_ONLY   0x10    /* NDEF is read-only            */
#define CE_T4T_STATUS_NDEF_FILE_UPDATING    0x20    /* NDEF is updating             */
#define CE_T4T_STATUS_WILDCARD_AID_SELECTED 0x40    /* Wildcard AID selected        */

    uint8_t status;

    tCE_CBACK *p_wildcard_aid_cback;    /* registered wildcard AID callback */
    tCE_T4T_REG_AID reg_aid[CE_T4T_MAX_REG_AID];    /* registered AID table             */
    uint8_t selected_aid_idx;
} tCE_T4T_MEM;

typedef struct {
    tCE_T3T_MEM t3t;
    tCE_T4T_MEM t4t;
} tCE_MEM;

typedef struct {
    tCE_MEM mem;
    tCE_CBACK *p_cback;
    uint8_t *p_ndef;    /* the memory starting from NDEF */
    uint16_t ndef_max;  /* max size of p_ndef */
    uint16_t ndef_cur;  /* current size of p_ndef */
    uint8_t tech;
    uint8_t trace_level;
} tCE_CB;

typedef UINT8 tNFC_PROTOCOL;
typedef UINT8 tNFC_BIT_RATE;

typedef struct
{
    UINT8       rats;  /* RATS */
} tNFC_INTF_LA_ISO_DEP;

#define NFC_MAX_ATS_LEN             60
#define NFC_MAX_HIS_BYTES_LEN       50
#define NFC_MAX_GEN_BYTES_LEN       48


typedef struct
{
    UINT8       ats_res_len;                /* Length of ATS RES                */
    UINT8       ats_res[NFC_MAX_ATS_LEN];   /* ATS RES                          */
    BOOLEAN     nad_used;                   /* NAD is used or not               */
    UINT8       fwi;                        /* Frame Waiting time Integer       */
    UINT8       sfgi;                       /* Start-up Frame Guard time Integer*/
    UINT8       his_byte_len;               /* len of historical bytes          */
    UINT8       his_byte[NFC_MAX_HIS_BYTES_LEN];/* historical bytes             */
} tNFC_INTF_PA_ISO_DEP;

#define NFC_MAX_ATTRIB_LEN  (10 + NFC_MAX_GEN_BYTES_LEN)

typedef struct
{
    UINT8       attrib_req_len;                /* Length of ATTRIB REQ      */
    UINT8       attrib_req[NFC_MAX_ATTRIB_LEN];/* ATTRIB REQ (Byte 2 - 10+k)*/
    UINT8       hi_info_len;                   /* len of Higher layer Info  */
    UINT8       hi_info[NFC_MAX_GEN_BYTES_LEN];/* Higher layer Info         */
    UINT8       nfcid0[NFC_NFCID0_MAX_LEN];    /* NFCID0                    */
} tNFC_INTF_LB_ISO_DEP;

typedef struct
{
    UINT8       attrib_res_len;                /* Length of ATTRIB RES      */
    UINT8       attrib_res[NFC_MAX_ATTRIB_LEN];/* ATTRIB RES                */
    UINT8       hi_info_len;                   /* len of Higher layer Info  */
    UINT8       hi_info[NFC_MAX_GEN_BYTES_LEN];/* Higher layer Info         */
    UINT8       mbli;                          /* Maximum buffer length.    */
} tNFC_INTF_PB_ISO_DEP;

typedef struct
{
    UINT8       atr_req_len;                /* Length of ATR_REQ            */
    UINT8       atr_req[NFC_MAX_ATS_LEN];   /* ATR_REQ (Byte 3 - Byte 18+n) */
    UINT8       max_payload_size;           /* 64, 128, 192 or 254          */
    UINT8       gen_bytes_len;              /* len of general bytes         */
    UINT8       gen_bytes[NFC_MAX_GEN_BYTES_LEN];/* general bytes           */
} tNFC_INTF_LA_NFC_DEP;

typedef struct
{
    UINT8       atr_res_len;                /* Length of ATR_RES            */
    UINT8       atr_res[NFC_MAX_ATS_LEN];   /* ATR_RES (Byte 3 - Byte 17+n) */
    UINT8       max_payload_size;           /* 64, 128, 192 or 254          */
    UINT8       gen_bytes_len;              /* len of general bytes         */
    UINT8       gen_bytes[NFC_MAX_GEN_BYTES_LEN];/* general bytes           */
    UINT8       waiting_time;               /* WT -> Response Waiting Time RWT = (256 x 16/fC) x 2WT */
} tNFC_INTF_PA_NFC_DEP;

typedef tNFC_INTF_PA_NFC_DEP tNFC_INTF_PF_NFC_DEP;

typedef tNFC_INTF_LA_NFC_DEP tNFC_INTF_LF_NFC_DEP;

#define NFC_MAX_RAW_PARAMS       16
typedef struct
{
    UINT8       param_len;
    UINT8       param[NFC_MAX_RAW_PARAMS];
} tNFC_INTF_FRAME;

typedef struct
{
    UINT8      type;  /* Interface Type  1 Byte  See Table 67 */
    union
    {
        tNFC_INTF_LA_ISO_DEP    la_iso;
        tNFC_INTF_PA_ISO_DEP    pa_iso;
        tNFC_INTF_LB_ISO_DEP    lb_iso;
        tNFC_INTF_PB_ISO_DEP    pb_iso;
        tNFC_INTF_LA_NFC_DEP    la_nfc;
        tNFC_INTF_PA_NFC_DEP    pa_nfc;
        tNFC_INTF_LF_NFC_DEP    lf_nfc;
        tNFC_INTF_PF_NFC_DEP    pf_nfc;
        tNFC_INTF_FRAME         frame;
    } intf_param;       /* Activation Parameters   0 - n Bytes */
} tNFC_INTF_PARAMS;

typedef struct
{
    UINT8                   rf_disc_id;     /* RF Discovery ID          */
    tNFC_PROTOCOL           protocol;       /* supported protocol       */
    tNFC_RF_TECH_PARAMS     rf_tech_param;  /* RF technology parameters */
    tNFC_DISCOVERY_TYPE     data_mode;      /* for future Data Exchange */
    tNFC_BIT_RATE           tx_bitrate;     /* Data Exchange Tx Bitrate */
    tNFC_BIT_RATE           rx_bitrate;     /* Data Exchange Rx Bitrate */
    tNFC_INTF_PARAMS        intf_param;     /* interface type and params*/
} tNFC_ACTIVATE_DEVT;

#define NFC_PROTOCOL_T2T    0x02	/* Type2Tag    - NFC-A            */


#define NFA_MAX_UID_LEN         0x0A
typedef struct
{
    BT_HDR              hdr;
    tNFA_PROTOCOL_MASK  protocol_mask;
    UINT8               *p_ndef_data;
    UINT16              ndef_cur_size;
    UINT16              ndef_max_size;
    BOOLEAN             read_only;
    UINT8               uid_len;
    UINT8               uid[NFA_MAX_UID_LEN];
} tNFA_CE_API_CFG_LOCAL_TAG;

typedef struct
{
    tNFC_STATUS             status;         /* The event status - place holder.  */
    UINT8                   rf_disc_id;     /* RF Discovery ID                   */
    UINT8                   protocol;       /* supported protocol                */
    tNFC_RF_TECH_PARAMS     rf_tech_param;  /* RF technology parameters          */
    UINT8                   more;           /* 0: last, 1: last (limit), 2: more */
} tNFC_RESULT_DEVT;

typedef struct
{
    tNFA_STATUS	        status;         /* NFA_STATUS_OK if successful       */
    tNFC_RESULT_DEVT    discovery_ntf;  /* RF discovery notification details */
} tNFA_DISC_RESULT;

#define NFA_T1T_HR_LEN          2
#define NFA_T1T_CMD_UID_LEN     4     /* the len of UID used in Type 1 Tag Commands     */

/* Data for NFA_ACTIVATED_EVT */
typedef struct
{
    UINT8               hr[NFA_T1T_HR_LEN];       /* HR of Type 1 tag         */
    UINT8               uid[NFA_T1T_CMD_UID_LEN]; /* UID used in T1T Commands */
} tNFA_T1T_PARAMS;

typedef struct
{
    UINT8               uid[NFA_MAX_UID_LEN];     /* UID of T2T tag           */
} tNFA_T2T_PARAMS;

typedef struct
{
    UINT8               num_system_codes;       /* Number of system codes supporte by tag   */
    UINT16              *p_system_codes;        /* Pointer to list of system codes          */
} tNFA_T3T_PARAMS;

#define I93_UID_BYTE_LEN                    8       /* UID length in bytes                  */
typedef struct
{
    UINT8               uid[I93_UID_BYTE_LEN];  /* UID[0]:MSB, ... UID[7]:LSB                   */
    UINT8               info_flags;             /* information flags                            */
    UINT8               dsfid;                  /* DSFID if I93_INFO_FLAG_DSFID                 */
    UINT8               afi;                    /* AFI if I93_INFO_FLAG_AFI                     */
    UINT16              num_block;              /* number of blocks if I93_INFO_FLAG_MEM_SIZE   */
    UINT8               block_size;             /* block size in byte if I93_INFO_FLAG_MEM_SIZE */
    UINT8               IC_reference;           /* IC Reference if I93_INFO_FLAG_IC_REF         */
} tNFA_I93_PARAMS;

typedef union
{
    tNFA_T1T_PARAMS     t1t;            /* HR and UID of T1T                */
    tNFA_T2T_PARAMS     t2t;            /* UID of T2T                       */
    tNFA_T3T_PARAMS     t3t;            /* System codes                     */
    tNFA_I93_PARAMS     i93;            /* System Information of ISO 15693  */
} tNFA_TAG_PARAMS;

typedef struct
{
    tNFC_ACTIVATE_DEVT  activate_ntf;   /* RF discovery activation details */
    tNFA_TAG_PARAMS     params;         /* additional informaiton of tag   */
} tNFA_ACTIVATED;

/* Data for NFA_DEACTIVATED_EVT */
typedef UINT8   tNFA_DEACTIVATE_TYPE;

typedef struct
{
    tNFA_DEACTIVATE_TYPE type;          /* NFA_DEACTIVATE_TYPE_IDLE or NFA_DEACTIVATE_TYPE_SLEEP */
} tNFA_DEACTIVATED;

typedef UINT8 tNFA_NFC_PROTOCOL;
typedef UINT8 tNFA_RW_NDEF_FLAG;
typedef struct
{
    tNFA_STATUS         status;             /* Status of the ndef detecton                              */
    tNFA_NFC_PROTOCOL   protocol;           /* protocol used to detect NDEF                             */
    UINT32              max_size;           /* max number of bytes available for NDEF data              */
    UINT32              cur_size;           /* current size of stored NDEF data (in bytes)              */
    tNFA_RW_NDEF_FLAG   flags;              /* Flags to indicate NDEF capability, is formated, soft/hard lockable, formatable, otp and read only */
} tNFA_NDEF_DETECT;

/* Structure for NFA_TLV_DETECT_EVT event data */
typedef struct
{
    tNFA_STATUS         status;     /* Status of the tlv detecton        */
    tNFA_NFC_PROTOCOL   protocol;   /* protocol used to detect TLV       */
    UINT8               num_tlvs;   /* number of tlvs present in the tag */
    UINT8               num_bytes;  /* number of lock/reserved bytes     */
} tNFA_TLV_DETECT;

/* Structure for NFA_DATA_EVT data */
typedef struct
{
    tNFA_STATUS         status;         /* Status of Data received          */
    UINT8               *p_data;        /* Data buffer                      */
    UINT16              len;            /* Length of data                   */
} tNFA_RX_DATA;

/* Structure for NFA_CE_NDEF_WRITE_CPLT_EVT data */
typedef struct
{
    tNFA_STATUS         status;         /* Status of the ndef write op      */
    UINT32              len;            /* Update length of NDEF data       */
    UINT8               *p_data;        /* data buffer                      */
} tNFA_CE_NDEF_WRITE_CPLT;

/* Data for NFA_LLCP_ACTIVATED_EVT */
typedef struct
{
    BOOLEAN             is_initiator;   /* TRUE if initiator                */
    UINT16              remote_wks;     /* Well-Known service mask of peer  */
    UINT8               remote_lsc;     /* Link Service Class of peer       */
    UINT16              remote_link_miu;/* Link MIU of peer                 */
    UINT16              local_link_miu; /* Link MIU of local                */
    UINT8               remote_version; /* LLCP version of remote           */
} tNFA_LLCP_ACTIVATED;

typedef struct
{
    UINT8               reason;         /* reason of deactivation           */
} tNFA_LLCP_DEACTIVATED;

typedef struct
{
    UINT8           dsfid;                  /* DSFID                       */
    UINT8           uid[I93_UID_BYTE_LEN];  /* UID[0]:MSB, ... UID[7]:LSB  */
} tNFA_I93_INVENTORY;

typedef struct                              /* RW_I93_SYS_INFO_EVT                          */
{
    UINT8           info_flags;             /* information flags                            */
    UINT8           uid[I93_UID_BYTE_LEN];  /* UID                                          */
    UINT8           dsfid;                  /* DSFID if I93_INFO_FLAG_DSFID                 */
    UINT8           afi;                    /* AFI if I93_INFO_FLAG_AFI                     */
    UINT16          num_block;              /* number of blocks if I93_INFO_FLAG_MEM_SIZE   */
    UINT8           block_size;             /* block size in byte if I93_INFO_FLAG_MEM_SIZE */
    UINT8           IC_reference;           /* IC Reference if I93_INFO_FLAG_IC_REF         */
} tNFA_I93_SYS_INFO;

typedef struct
{
    tNFA_STATUS         status;         /* Status of sending command       */
    UINT8               sent_command;   /* sent command to tag             */
    union
    {
        UINT8               error_code; /* error code defined in ISO 15693 */
        tNFA_I93_INVENTORY  inventory;  /* inventory response              */
        tNFA_I93_SYS_INFO   sys_info;   /* system information              */
    } params;
} tNFA_I93_CMD_CPLT;

typedef UINT16 tNFA_HANDLE;

/* Data for NFA_CE_REGISTERED_EVT */
typedef struct
{
    tNFA_STATUS         status;         /* NFA_STATUS_OK if successful                      */
    tNFA_HANDLE         handle;         /* handle for NFA_CeRegisterFelicaSystemCodeOnDH () */
                                        /*            NFA_CeRegisterT4tAidOnDH ()           */
} tNFA_CE_REGISTERED;

typedef struct
{
    tNFA_HANDLE         handle;         /* handle from NFA_CE_REGISTERED_EVT   */
} tNFA_CE_DEREGISTERED;

/* Data for NFA_CE_ACTIVATED_EVT */
typedef struct
{
    tNFA_STATUS         status;         /* NFA_STATUS_OK if successful              */
    tNFA_HANDLE         handle;         /* handle from NFA_CE_REGISTERED_EVT        */
    tNFC_ACTIVATE_DEVT  activate_ntf;   /* RF discovery activation details          */
} tNFA_CE_ACTIVATED;

/* Data for NFA_CE_DEACTIVATED_EVT */
typedef struct
{
    tNFA_HANDLE         handle;         /* handle from NFA_CE_REGISTERED_EVT   */
    tNFA_DEACTIVATE_TYPE type;          /* NFA_DEACTIVATE_TYPE_IDLE or NFA_DEACTIVATE_TYPE_SLEEP */
} tNFA_CE_DEACTIVATED;

/* Structure for NFA_CE_DATA_EVT data */
typedef struct
{
    tNFA_STATUS         status;         /* NFA_STATUS_OK if complete packet     */
    tNFA_HANDLE         handle;         /* handle from NFA_CE_REGISTERED_EVT    */
    UINT8               *p_data;        /* Data buffer                          */
    UINT16              len;            /* Length of data                       */
} tNFA_CE_DATA;

typedef union
{
    tNFA_STATUS             status;             /* NFA_POLL_ENABLED_EVT                 */
                                                /* NFA_POLL_DISABLED_EVT                */
                                                /* NFA_CE_UICC_LISTEN_CONFIGURED_EVT    */
                                                /* NFA_EXCLUSIVE_RF_CONTROL_STARTED_EVT */
                                                /* NFA_EXCLUSIVE_RF_CONTROL_STOPPED_EVT */
                                                /* NFA_SELECT_RESULT_EVT                */
                                                /* NFA_DEACTIVATE_FAIL_EVT              */
                                                /* NFA_CE_NDEF_WRITE_START_EVT          */
                                                /* NFA_SELECT_CPLT_EVT                  */
                                                /* NFA_READ_CPLT_EVT                    */
                                                /* NFA_WRITE_CPLT_EVT                   */
                                                /* NFA_PRESENCE_CHECK_EVT               */
                                                /* NFA_FORMAT_CPLT_EVT                  */
                                                /* NFA_SET_TAG_RO_EVT                   */
                                                /* NFA_UPDATE_RF_PARAM_RESULT_EVT       */
                                                /* NFA_RW_INTF_ERROR_EVT                */
    tNFA_DISC_RESULT         disc_result;       /* NFA_DISC_RESULT_EVT                  */
    tNFA_ACTIVATED           activated;         /* NFA_ACTIVATED_EVT                    */
    tNFA_DEACTIVATED         deactivated;       /* NFA_DEACTIVATED_EVT                  */
    tNFA_NDEF_DETECT         ndef_detect;       /* NFA_NDEF_DETECT_EVT                  */
    tNFA_TLV_DETECT          tlv_detect;        /* NFA_TLV_DETECT_EVT                   */
    tNFA_RX_DATA             data;              /* NFA_DATA_EVT                         */
    tNFA_CE_NDEF_WRITE_CPLT  ndef_write_cplt;   /* NFA_CE_NDEF_WRITE_CPLT_EVT           */
    tNFA_LLCP_ACTIVATED      llcp_activated;    /* NFA_LLCP_ACTIVATED_EVT               */
    tNFA_LLCP_DEACTIVATED    llcp_deactivated;  /* NFA_LLCP_DEACTIVATED_EVT             */
    tNFA_I93_CMD_CPLT        i93_cmd_cplt;      /* NFA_I93_CMD_CPLT_EVT                 */
    tNFA_CE_REGISTERED       ce_registered;     /* NFA_CE_REGISTERED_EVT                */
    tNFA_CE_DEREGISTERED     ce_deregistered;   /* NFA_CE_DEREGISTERED_EVT              */
    tNFA_CE_ACTIVATED        ce_activated;      /* NFA_CE_ACTIVATED_EVT                 */
    tNFA_CE_DEACTIVATED      ce_deactivated;    /* NFA_CE_DEACTIVATED_EVT               */
    tNFA_CE_DATA             ce_data;           /* NFA_CE_DATA_EVT                      */

} tNFA_CONN_EVT_DATA;

typedef void (tNFA_CONN_CBACK) (UINT8 event, tNFA_CONN_EVT_DATA *p_data);
typedef UINT8 tNFA_CE_REG_TYPE;
typedef UINT16 tNFA_HANDLE;
typedef UINT8 tNFA_TECHNOLOGY_MASK;
typedef struct
{
    BT_HDR              hdr;
    tNFA_CONN_CBACK     *p_conn_cback;

    tNFA_CE_REG_TYPE   listen_type;

    /* For registering Felica */
    UINT16              system_code;
    UINT8               nfcid2[NCI_RF_F_UID_LEN];

    /* For registering Type-4 */
    UINT8               aid[NFC_MAX_AID_LEN];   /* AID to listen for (For type-4 only)  */
    UINT8               aid_len;                /* AID length                           */

    /* For registering UICC */
    tNFA_HANDLE             ee_handle;
    tNFA_TECHNOLOGY_MASK    tech_mask;
} tNFA_CE_API_REG_LISTEN;

/* data type for NFA_CE_API_DEREG_LISTEN_EVT */
typedef struct
{
    BT_HDR          hdr;
    tNFA_HANDLE     handle;
    UINT32          listen_info;
} tNFA_CE_API_DEREG_LISTEN;

/* data type for NFA_CE_ACTIVATE_NTF_EVT */
typedef struct
{
    BT_HDR              hdr;
    tNFC_ACTIVATE_DEVT *p_activation_params;
} tNFA_CE_ACTIVATE_NTF;

typedef union
{
    /* GKI event buffer header */
    BT_HDR                      hdr;
    tNFA_CE_API_CFG_LOCAL_TAG   local_tag;
    tNFA_CE_API_REG_LISTEN      reg_listen;
    tNFA_CE_API_DEREG_LISTEN    dereg_listen;
    tNFA_CE_ACTIVATE_NTF        activate_ntf;
} tNFA_CE_MSG;

typedef UINT32 tNFA_CE_FLAGS;

typedef struct
{
    UINT32              flags;
    tNFA_CONN_CBACK     *p_conn_cback;                  /* Callback for this listen request             */
    tNFA_PROTOCOL_MASK  protocol_mask;                  /* Mask of protocols for this listen request    */
    tNFA_HANDLE         rf_disc_handle;                 /* RF Discover handle */

    /* For host tag emulation (NFA_CeRegisterVirtualT4tSE and NFA_CeRegisterT4tAidOnDH) */
    UINT8               t3t_nfcid2[NCI_RF_F_UID_LEN];
    UINT16              t3t_system_code;                /* Type-3 system code */
    UINT8               t4t_aid_handle;                 /* Type-4 aid callback handle (from CE_T4tRegisterAID) */

    /* For UICC */
    tNFA_HANDLE                     ee_handle;
    tNFA_TECHNOLOGY_MASK            tech_mask;          /* listening technologies               */
    tNFA_DM_DISC_TECH_PROTO_MASK    tech_proto_mask;    /* listening technologies and protocols */
} tNFA_CE_LISTEN_INFO;

#define NFA_CE_LISTEN_INFO_MAX        5

typedef BOOLEAN (tNFA_SYS_EVT_HDLR) (BT_HDR *p_msg);

/* NFA_CE control block */
typedef struct
{
    UINT8   *p_scratch_buf;                                 /* Scratch buffer for write requests    */
    UINT32  scratch_buf_size;

    tNFC_ACTIVATE_DEVT  activation_params;                  /* Activation params        */
    tNFA_CE_FLAGS       flags;                              /* internal flags           */
    tNFA_CONN_CBACK     *p_active_conn_cback;               /* Callback of activated CE */

    /* listen_info table (table of listen paramters and app callbacks) */
    tNFA_CE_LISTEN_INFO listen_info[NFA_CE_LISTEN_INFO_MAX];/* listen info table                            */
    UINT8               idx_cur_active;                     /* listen_info index for currently activated CE */
    UINT8               idx_wild_card;                      /* listen_info index for T4T wild card CE */

    tNFA_DM_DISC_TECH_PROTO_MASK isodep_disc_mask;          /* the technology/protocol mask for ISO-DEP */

    /* Local ndef tag info */
    UINT8               *p_ndef_data;
    UINT16              ndef_cur_size;
    UINT16              ndef_max_size;

    tNFA_SYS_EVT_HDLR   *p_vs_evt_hdlr;                     /* VS event handler */
} tNFA_CE_CB;

#define NFA_TECHNOLOGY_MASK_A	                0x01    /* NFC Technology A             */
#define NFA_DM_DISC_MASK_LA_ISO_DEP       0x00040000
#define NFA_TECHNOLOGY_MASK_B	                0x02    /* NFC Technology B             */
#define NFA_DM_DISC_MASK_LB_ISO_DEP       0x00100000

#define NFA_CE_LISTEN_INFO_IDX_INVALID             5
#define NFA_PROTOCOL_T2T                        0x02    /* MIFARE/Type2Tag  - NFC-A             */

#define NFA_CE_LISTEN_INFO_IN_USE         0x00000001    /* LISTEN_INFO entry is in use */
#define NFA_PROTOCOL_MASK_T1T                   0x01    /* Type 1 tag          */
#define NFA_PROTOCOL_MASK_T2T                   0x02    /* MIFARE / Type 2 tag */
#define NFA_PROTOCOL_MASK_ISO_DEP               0x08    /* ISODEP/4A,4B        */
#define NFA_CE_LISTEN_INFO_T4T_ACTIVATE_PND 0x00000040  /* App has not been notified of ACTIVATE_EVT yet for this T4T AID   */

#define NFA_ACTIVATED_EVT                       5   /* NFC link/protocol activated */

/* SW sub-systems */
enum {
    NFA_ID_SYS,         /* system manager                      */
    NFA_ID_DM,          /* device manager                      */
    NFA_ID_EE,          /* NFCEE sub-system                    */
    NFA_ID_P2P,         /* Peer-to-Peer sub-system             */
    NFA_ID_CHO,         /* Connection Handover sub-system      */
    NFA_ID_SNEP,        /* SNEP sub-system                     */
    NFA_ID_RW,          /* Reader/writer sub-system            */
    NFA_ID_CE,          /* Card-emulation sub-system           */
    NFA_ID_HCI,         /* Host controller interface sub-system*/
    NFA_ID_DTA,         /* Device Test Application sub-system  */
    NFA_ID_MAX
};

#define NFA_SYS_EVT_START(id)       ((id) << 8)

/* CE events */
enum
{
    /* device manager local device API events */
    NFA_CE_API_CFG_LOCAL_TAG_EVT    = NFA_SYS_EVT_START (NFA_ID_CE),
    NFA_CE_API_REG_LISTEN_EVT,
    NFA_CE_API_DEREG_LISTEN_EVT,
    NFA_CE_API_CFG_ISODEP_TECH_EVT,
    NFA_CE_ACTIVATE_NTF_EVT,
    NFA_CE_DEACTIVATE_NTF_EVT,

    NFA_CE_MAX_EVT
};

/* DM events */
enum
{
    /* device manager local device API events */
    NFA_DM_API_ENABLE_EVT           = NFA_SYS_EVT_START (NFA_ID_DM),
    NFA_DM_API_DISABLE_EVT,
    NFA_DM_API_SET_CONFIG_EVT,
    NFA_DM_API_GET_CONFIG_EVT,
    NFA_DM_API_REQUEST_EXCL_RF_CTRL_EVT,
    NFA_DM_API_RELEASE_EXCL_RF_CTRL_EVT,
    NFA_DM_API_ENABLE_POLLING_EVT,
    NFA_DM_API_DISABLE_POLLING_EVT,
    NFA_DM_API_ENABLE_LISTENING_EVT,
    NFA_DM_API_DISABLE_LISTENING_EVT,
    NFA_DM_API_PAUSE_P2P_EVT,
    NFA_DM_API_RESUME_P2P_EVT,
    NFA_DM_API_RAW_FRAME_EVT,
    NFA_DM_API_SET_P2P_LISTEN_TECH_EVT,
    NFA_DM_API_START_RF_DISCOVERY_EVT,
    NFA_DM_API_STOP_RF_DISCOVERY_EVT,       // to be used by NFA_StopRfDiscovery()
    NFA_DM_API_SET_RF_DISC_DURATION_EVT,
    NFA_DM_API_SELECT_EVT,
    NFA_DM_API_UPDATE_RF_PARAMS_EVT,
    NFA_DM_API_DEACTIVATE_EVT,
    NFA_DM_API_POWER_OFF_SLEEP_EVT,
    NFA_DM_API_REG_NDEF_HDLR_EVT,
    NFA_DM_API_DEREG_NDEF_HDLR_EVT,
    NFA_DM_API_REG_VSC_EVT,
    NFA_DM_API_SEND_VSC_EVT,
    NFA_DM_TIMEOUT_DISABLE_EVT,
    NFA_DM_MAX_EVT
};

/* Listen registration types */
enum
{
    NFA_CE_REG_TYPE_NDEF,
    NFA_CE_REG_TYPE_ISO_DEP,
    NFA_CE_REG_TYPE_FELICA,
    NFA_CE_REG_TYPE_UICC
};