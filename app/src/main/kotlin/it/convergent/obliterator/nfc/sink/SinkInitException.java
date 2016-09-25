package it.convergent.obliterator.nfc.sink;

/**
 * Exception if something goes wrong with Sink Initialization
 */
public class SinkInitException extends Exception {
    public SinkInitException(String message) {
        super(message);
    }
}
