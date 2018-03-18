package cz.marw.indoorlocalization.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martinek on 13. 3. 2018.
 */

public class Scan {

    private int ageOfScan;
    private byte flag;
    private List<RadioPrint> prints = new ArrayList<>();

    public void addRadioPrint(String macAddr, int rssi, int discoveryTime) {
        prints.add(new RadioPrint(macAddr, rssi, discoveryTime));
    }

    public void addRadioPrint(RadioPrint radioPrint) {
        prints.add(radioPrint);
    }

    public int getTotalCount() {
        return prints.size();
    }

    public int getAgeOfScan() {
        return ageOfScan;
    }

    public void setAgeOfScan(int ageOfScan) {
        this.ageOfScan = ageOfScan;
    }

    public byte getFlag() {
        return flag;
    }

    public void setFlag(byte flag) {
        this.flag = flag;
    }

    public List<RadioPrint> getPrints() {
        return prints;
    }

    public void setPrints(List<RadioPrint> prints) {
        this.prints = prints;
    }
}
