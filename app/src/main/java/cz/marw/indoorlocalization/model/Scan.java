package cz.marw.indoorlocalization.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Martinek on 13. 3. 2018.
 */

public class Scan {

    private int ageOfScan;
    private byte flag;
    private List<RadioPrint> prints = new ArrayList<>();
    private int scanDuration;
    private final Date startOfScan;

    public Scan() {
        startOfScan = new Date();
    }

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

    public int getScanDuration() {
        return scanDuration;
    }

    public void setScanDuration(int scanDuration) {
        this.scanDuration = scanDuration;
    }

    public String getFormattedStartOfScan(String format) {
        String formatted = "";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        formatted = sdf.format(startOfScan);

        return formatted;
    }

    public Date getStartOfScan() {
        return startOfScan;
    }

}
