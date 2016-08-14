package it.convergent.obliterator

/**
 * Created by altamic on 10/04/16.
 */
abstract class Constants {

    /*   Carnet Structure
     *   ================
     *
     *   Page 0x00: 0x04XXYYZZ    checksum: 0xZZ = 0x88 ⊕ 0x04 ⊕ 0xXX ⊕ 0xYY
     *   Page 0x01: 0xGGHHIIJJ    UID: CoP = 0x04XXYYGGHHIIJJ in decimal
     *   Page 0x02: 0xLL          checksum: 0xLL = 0xGG ⊕ 0xHH ⊕ 0xII ⊕ 0xJJ
     *                  KK        internal: 0xKK i.e. chip vendor identifier
     *                    F2      Lock byte #1:
     *                                         0xF:         1            1               1              1
     *                                       meaning:   Page7locked, Page6locked,   Page5locked, Page4locked
     *
     *                                         0x2:         0            0               1              0
     *                                       meaning:  OTPlocked, Pages10to15locked, Pages4to9locked, OTPlocked
     *
     *                      03    Lock byte #2:
     *                                         0x0:         0            0               0              0
     *                                       meaning:  Page15locked, Page14locked, Page13locked, Page12locked
     *
     *                                         0x3:         0            0               1              1
     *                                       meaning:  Page11locked, Page10locked,  Page9locked,  Page8locked
     *
     *   Page 0x03: 0xMMNNOOPP    OTP: remaining rides
     *   Page 0x04: 0x01LL0000    Layout -> Carnet rides
     *   Page 0x05: 0xQQ01RRRR    Mask, Tariff
     *   Page 0x06: 0xTTTTTT00    Purchase date in minutes since GTT epoch
     *   Page 0x07: 0x00######
     *   Page 0x08: 0x##00%%%%    Purchase serial number: ########-%%%% where %%%% in decimal
     *   Page 0x09: 0x????????    Unknown: maybe random
     *   Page 0x0A: 0xTTTTTT00    First timestamp in minutes since GTT epoch
     *   Page 0x0B: 0x04F80000    GTT Zone
     *   Page 0x0C: 0xTTTTTT00    Last timestamp in minutes since GTT epoch within 90'
     *   Page 0x0D: 0x00PP00??    GTT bus line
     *   Page 0x0E: 0xF8AE1???
     *   Page 0x0F: 0x??12????    Unknown, it depends perhaps at least from last stamp within 90'
     *
     *   Factory read only pages: from 0x00 to 0x03
     *   Lock bytes (page 0x02) control remaining pages
     *
     */

    val PAGE_SIZE = 4
    val TOTAL_PAGES = 16

    val OTP_OFFSET = 0x03
    val DATE_TIME_OFFSET = 0x0A
    val DATE_TIME_BEFORE_EXPIRY_OFFSET = 0x0C
    val MAX_REMAINING_MINUTES = 90

    // 01 rides Carnet
    val _01_RIDES_MINUS_00 = 0b00111111111111111111111111111110 // 0x3FFFFFFE ??
    val _01_RIDES_MINUS_01 = 0b01111111111111111111111111111111 // 0x7FFFFFFF ??

    // 5 rides Carnet
    val _05_RIDES_TYPE_PAGE_4 = 0x01040000
    val _05_RIDES_TYPE_PAGE_5 = 0x020102BE
    val _05_RIDES_MINUS_00 = 0b00000111111111111111111111100000 // 0x07FFFFE0
    val _05_RIDES_MINUS_01 = 0b00000111111111111111111111110000 // 0x07FFFFF0 ??
    val _05_RIDES_MINUS_02 = 0b00001111111111111111111111111000 // 0x0FFFFFF8 ??
    val _05_RIDES_MINUS_03 = 0b00011111111111111111111111111100 // 0x1FFFFFFC ??
    val _05_RIDES_MINUS_04 = 0b00111111111111111111111111111110 // 0x3FFFFFFE ??
    val _05_RIDES_MINUS_05 = 0b01111111111111111111111111111111 // 0x7FFFFFFF

    // 15 rides Carnet
    val _15_RIDES_TYPE_PAGE_4 = 0x01050000
    val _15_RIDES_TYPE_PAGE_5 = 0x020102BD
    val _15_RIDES_MINUS_00 = 0b00000000000000011000000000000000 // 0x00018000
    val _15_RIDES_MINUS_01 = 0b00000000000000011100000000000000 // 0x0001C000
    val _15_RIDES_MINUS_02 = 0b00000000000000111110000000000000 // 0x0003E000
    val _15_RIDES_MINUS_03 = 0b00000000000001111111000000000000 // 0x0007F000
    val _15_RIDES_MINUS_04 = 0b00000000000011111111100000000000 // 0x000FF800
    val _15_RIDES_MINUS_05 = 0b00000000000111111111110000000000 // 0x001FFC00
    val _15_RIDES_MINUS_06 = 0b00000000001111111111111000000000 // 0x003FFE00
    val _15_RIDES_MINUS_07 = 0b00000000011111111111111100000000 // 0x007FFF00
    val _15_RIDES_MINUS_08 = 0b00000000111111111111111110000000 // 0x00FFFF80
    val _15_RIDES_MINUS_09 = 0b00000001111111111111111111000000 // 0x01FFFFC0
    val _15_RIDES_MINUS_10 = 0b00000011111111111111111111100000 // 0x03FFFFE0
    val _15_RIDES_MINUS_11 = 0b00000111111111111111111111110000 // 0x07FFFFF0
    val _15_RIDES_MINUS_12 = 0b00001111111111111111111111111000 // 0x0FFFFFF8
    val _15_RIDES_MINUS_13 = 0b00011111111111111111111111111100 // 0x1FFFFFFC
    val _15_RIDES_MINUS_14 = 0b00111111111111111111111111111110 // 0x3FFFFFFE
    val _15_RIDES_MINUS_15 = 0b01111111111111111111111111111111 // 0x7FFFFFFF

    val metroStopArray: Array<String> = arrayOf(
            "Fermi", "Paradiso", "Marche", "Massaua",
            "Pozzo Strada", "Monte Grappa", "Rivoli",
            "Racconigi", "Bernini", "Principi d'Acaja",
            "XVIII Dicembre", "Porta Susa", "Vinzaglio",
            "Re Umberto", "Porta Nuova FS", "Marconi",
            "Nizza", "Dante", "Carducci-Molinette",
            "Spezia", "Lingotto")
}