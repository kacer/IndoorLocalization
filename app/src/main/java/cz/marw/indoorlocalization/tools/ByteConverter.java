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

}
