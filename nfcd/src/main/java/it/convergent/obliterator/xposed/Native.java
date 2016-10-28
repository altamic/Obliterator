package it.convergent.obliterator.xposed;

/**
 * Created by altamic on 23/10/16.
 */

public class Native {
    static {
        Instance = new Native();
    }
    public static final Native Instance;
    public native void uploadConfiguration(byte atqa, byte sak, byte[] uid, byte[] hist, byte[] data);
    public native void setEnabled(boolean enabled);
    public native boolean isEnabled();
}
