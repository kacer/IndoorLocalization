package cz.marw.indoorlocalization.managers;

import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cz.marw.indoorlocalization.model.RadioPrint;
import cz.marw.indoorlocalization.model.Scan;

/**
 * Created by Martinek on 27. 3. 2018.
 */

public class DataExportManager {

    private static final String BASE_DIR_NAME = "SensorTag";

    private File dataDir;

    public DataExportManager() {
        File externalStorageDir = Environment.getExternalStorageDirectory();
        dataDir = new File(externalStorageDir.getAbsolutePath(), BASE_DIR_NAME);
    }

    public void exportData(Scan scan) {
        if(!checkStorage()) {

            return;
        }

        try {
            FileWriter writer = new FileWriter(new File(dataDir, getFilename()));

            JSONObject scanJson = new JSONObject();
            JSONArray radioPrints = new JSONArray();

            scanJson.put("ageOfScan", scan.getAgeOfScan());
            scanJson.put("flag", scan.getFlag());
            scanJson.put("totalCount", scan.getTotalCount());

            for(RadioPrint rp : scan.getPrints()) {
                JSONObject radioPrint = new JSONObject();

                radioPrint.put("mac", rp.getMacAddr());
                radioPrint.put("rssi", rp.getRssi());
                radioPrint.put("discoveryTime", rp.getDiscoveryTime());

                radioPrints.put(radioPrint);
            }

            scanJson.put("radioPrints", radioPrints);

            writer.write(scanJson.toString(1));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private boolean checkStorage() {
        if(!dataDir.exists())
            dataDir.mkdir();
        
        String storageState = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED.equals(storageState);
    }

    private String getFilename() {
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String date = df.format(new Date());

        return "Scanning " + date + ".json";
    }

}
