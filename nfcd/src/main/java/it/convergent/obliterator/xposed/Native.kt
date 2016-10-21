package it.convergent.obliterator.xposed

class Native {
    companion object {
        val Instance = Native()
    }

    external fun setEnabled(value: Boolean): Void
    external fun isEnabled(): Boolean
    external fun uploadConfiguration(atqa: Byte, sak: Byte, hist: ByteArray, uid: ByteArray)
}
