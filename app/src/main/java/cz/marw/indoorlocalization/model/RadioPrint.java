package cz.marw.indoorlocalization.model;

/**
 * Created by Martinek on 13. 3. 2018.
 */

public class RadioPrint {

    private String macAddr;
    private int rssi;
    private int discoveryTime;

    public RadioPrint(String macAddr, int rssi, int discoveryTime) {
        this.macAddr = macAddr;
        this.rssi = rssi;
        this.discoveryTime = discoveryTime;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public String getMacAddr() {
        return macAddr;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }

    public int getDiscoveryTime() {
        return discoveryTime;
    }

    public void setDiscoveryTime(int discoveryTime) {
        this.discoveryTime = discoveryTime;
    }
}
