//
// Created by altamic on 21/09/16.
//

#include "command_interpreter.h"

#include <stdint.h>

#define PAGE_SIZE   0x04
#define TOTAL_PAGES 0x0F

typedef enum {
    POR_STATE, IDLE_STATE, HALT_STATE,
    READY_1_STATE, READY_2_STATE, ACTIVE_STATE
} State;

typedef enum {
    INIT, REQA, WUPA,
    ANTICOLLISION_CL1, SELECT_CL1,
    ANTICOLLISION_CL2,SELECT_CL2,
    HALT,
    READ, WRITE, C_WRITE
} Command; // from PCD

// 80 μs = 0.08 ms
typedef enum {
    ATQA,
    UID_1, SAK_CL1,
    UID_2, SAK_CL2,
    PAGES
} Response // from PICC

// Commands
uint8_t          REQA[] = { 0x26 };
uint8_t          WUPA[] = { 0x52 };
// Response
uint8_t          ATQA[] = { 0x44, 0x00 };

// Command ANTICOLLISION_CL1 = 0x93(rand between 0x20..0x67)
uint8_t ANTICOLLISION_CL1[] = { 0x93, 0x20 }; // rand between 0x20..0x67
// Response
uint8_t         UID_1[] = { 0x88, 0x04, 0xB8, 0x98, 0xAC }; // CT, UID(byte 1,2,3), BCC

// Command SELECT_CL1 = 0x93(0x70 0x88 SN0 SN1 SN2 BCC1 C0 C1)
uint8_t        SELECT_CL1[] = { 0x93, 0x70, 0x88, 0x04, 0xB8, 0x98, 0xAC, 0x0E, 0x05 };
// Response SAK_CL1 = 0x04 C0 C1
uint8_t           SAK_CL1[] = { 0x04, 0x0E, 0x05 };

// Command
uint8_t ANTICOLLISION_CL2[] = { 0x95, 0x20 }; // rand between 0x20..0x67
// Response
uint8_t         UID_2[] = { 0x6A, 0x70, 0x33, 0x80, 0xA9 }; // UID(byte 4,5,6,7), BCC

// Command SELECT_CL2 = 0x95(0x70 SN3 SN4 SN5 SN6 BCC2 C0 C1)
uint8_t        SELECT_CL2[] = { 0x95, 0x70, 0x6A, 0x70, 0x33, 0x80, 0xA9, 0xB9, 0xE1 };
// Response SAK_CL2 = 00h C0 C1
uint8_t           SAK_CL2[] = { 0x00, 0xB9, 0xE1 };

// Command READ(PAGE_INDEX) = 0x30(0x00..0x0F, C0, C1)
uint8_t             READ[] = { 0x30, 0x00, 0xF3, 0x8A };
// Response PAGE_INDEX..PAGE_INDEX + 4 mod(0x0F) (4 pages)
uint8_t             PAGES[] = { 0x04, 0xB8, 0x98, 0xAC,
                                0x6A, 0x70, 0x33, 0x80,
                                0xA9, 0x48, 0xF2, 0x03,
                                0x1F, 0xFF, 0xFF, 0xFC,
                                0x54, 0x1F }
// or NAK if ADDR is not between 0x00..0x0F
uint8_t          NACK[] = { 0x15 }


// Command HALT = 0x50(0x00 C0 C1)
uint8_t          HALT[] = { 0x50, 0x00, 0x57, 0xCD };
// Response ACK / NAK
uint8_t           ACK[] = { 0x06 }

// Command WRITE = 0xA2(ADDR D0 D1 D2 D3 C0 C1) | ADDR ∈ 0x00..0x0F
uint8_t         WRITE[] = { 0xA2, 0x00, 0xCA, 0xFE, 0xBA, 0xBE, 0x12, 0x32 };

// Response ACK (after 3830 μs)/ NAK (immediately)

// Command C_WRITE = 0xA0(ADDR C0 C1 D0 D1 D2 D3 ... D15 C0 C1) | ADDR ∈ 0x00..0x0F
// write D0 D1 D2 D3 at the ADDR
uint8_t       C_WRITE[] = { 0xA2, 0x00, 0x12, 0x43,
                            0xCA, 0xFE, 0xBA, 0xBE,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x42, 0xE5 };
// Response ACK (after 3830 μs)
// or NAK (after 80 μs past the 4th byte or the end of the command)


// CRC_A =
State current_state;

uint8_t data[TOTAL_PAGES][PAGE_SIZE];

void initialize(uint32_t initial_data[]) {
    for (int i = 0; i < TOTAL_PAGES; i++) {
        data[i] = initial_data[i];
    }

    State current_state = POR_STATE;
}

void next_state(Command c) {

}


int main(int argc, char *args[]) {
    return 0;
}

void transition_to(Command c) {

}

void append_iso14443a_crc(uint8_t *data, size_t len) {
    iso14443a_crc(data, len, data + len)
}

void iso14443a_crc(uint8_t *pbtData, size_t szLen, uint8_t *pbtCrc) {
  uint8_t  bt;
  uint32_t wCrc = 0x6363;

  do {
    bt = *pbtData++;
    bt = (bt ^ (uint8_t)(wCrc & 0x00FF));
    bt = (bt ^ (bt << 4));
    wCrc = (wCrc >> 8) ^ ((uint32_t) bt << 8) ^ ((uint32_t) bt << 3) ^ ((uint32_t) bt >> 4);
  } while (--szLen);

  *pbtCrc++ = (uint8_t)(wCrc & 0xFF);
  *pbtCrc = (uint8_t)((wCrc >> 8) & 0xFF);
}


