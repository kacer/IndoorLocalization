package cz.marw.indoorlocalization.tools;

/**
 * Created by Martinek on 27. 3. 2018.
 */

public class MacAddress {

    public static String macAsString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        if(bytes != null && bytes.length > 0) {
            for(int i = 5; i >= 0; i--) {
                sb.append(String.format("%02X", bytes[i]));
                if(i != 0)
                    sb.append(":");
            }

        }

        return sb.toString();
    }

}
