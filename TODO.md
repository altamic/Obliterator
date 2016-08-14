TODO
====

Obliterator Mode
----------------

### Graphics

* check exceptions during tag reading (tag has been removed abruptly and so on)
* produce vector graphics to draw the obliterator
* produce vector graphics to identify a carnet
* change colors of lateral button on pressed
* info button default message
* draw 3 LEDs (green ok, red ko)
* record sounds
* determine messages

### Logic

* determine if there is a MAC on a ticket
* test simple f(AES, key, padding, IV) 


Carnet Mode
-----------

### Graphics

### Logic 

* find a phone to test: Nexus 4, Nexus 5, G2
* find API to call for bcm2079x chips for changing UID (custom, 7 bytes length)


MIFARE Ultralight
=================

State Machine
-------------

States:    { POR, IDLE, HALT, READY_1, READY_2, ACTIVE } 
Events:    { INIT, REQA, WUPA, ANTICOLLISION, 
             SELECT_CL1, SELECT_CL2, HALT,
             READ(a), WRITE(a), C_WRITE(a) | a ∈ Addresses,
             UNEXPECTED }
Edges:     { INIT(POR, IDLE), REQA(IDLE -> READY_1), WUPA(IDLE -> READY_1), 
             WUPA(HALT -> READY_1), ANTICOLLISION(READY_1 -> READY_1),
             READ(0)(READY_1 -> ACTIVE), SELECT_CL1(READY_1 -> READY_2), 
             ANTICOLLISION(READY_2 -> READY_2), SELECT_CL2(READY_2 -> ACTIVE),
             WRITE(ACTIVE -> ACTIVE), READ(a)(ACTIVE -> ACTIVE),
             HALT(ACTIVE -> HALT),
             UNEXPECTED(x -> IDLE) a ∈ States / { HALT }, 
             UNEXPECTED(HALT -> HALT) }

Addresses: { 0x0, ..., 0xF }

In all states, the command interpreter will return to the idle state on receipt 
of an unexpected command. If the IC was previously in the halt state, it will 
return to that state.

