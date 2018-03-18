package cz.marw.indoorlocalization.tools;

/**
 * Created by Martinek on 14. 3. 2018.
 */

public class ByteConverter {

    public static final int FORMAT_UINT8 = 0;
    public static final int FORMAT_UINT16 = 1;
    public static final int FORMAT_UINT32 = 2;

    public static byte[] intToBytes(int value, int formatType) {
        byte[] bytes = null;

        switch(formatType) {
            case FORMAT_UINT8:
                break;
            case FORMAT_UINT16:
                bytes = new byte[2];

                bytes[0] = (byte) ((value >> 8) & 0xFF);
                bytes[1] = (byte) (value & 0xFF);
                break;
            case FORMAT_UINT32:
                break;
        }

        return bytes;
    }

    public static int bytesToInt(byte[] bytes, int formatType) {
        int value = 0;

        switch(formatType) {
            case FORMAT_UINT8:
                value = bytes[0];
                break;
            case FORMAT_UINT16:
                value = (bytes[0] << 8) + bytes[1];
                break;
            case FORMAT_UINT32:
                break;
        }

        return value;
    }

}
