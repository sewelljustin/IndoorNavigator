package com.js.indoornavigator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class NavigatorView extends View {

    private static final String TAG = "IndoorNavigator";
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;


    // SCAN FIELDS
    // scan settings
    private static final ScanSettings SCAN_SETTINGS =
            new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0)
                    .build();

    // service UUID
    private static final ParcelUuid EDDYSTONE_SERVICE_UUID =
            ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");

    private BluetoothLeScanner scanner;

    private List<ScanFilter> scanFilters;
    private ScanCallback scanCallback;

    // Shared preferences
    private SharedPreferences sharedPreferences;

    // Booleans for the different methods used
    private boolean highestRssiMethod;
    private boolean density1Method;
    private boolean density2Method;
    private boolean bayesMethod;

    // Holds highestRssi value
    private int highestRssi;

    // Holds largest sample count
    private int largestSampleCount;

    // Time variables used for density2Method
    private double startTime;
    private double elaspedTime;
    private final int TIME_INTERVAL = 500;

    // holds the current quadrant of the device
    private int currentPosition;

    // used with Bayes theorem algorithm
    private int previousPosition;

    // TEST USING BEACONS FOR CURRENT AND PREVIOUS POSITIONS
    private Beacon currentBeacon;
    private Beacon previousBeacon;

    // Constants
    private final int SAMPLE_SIZE = 10;

    // Holds the beacons with the largest RSSI values over a time interval
    private Queue<Beacon> sample;

    // Used to draw initial grid to canvas
    boolean gridDrawn;

    // Fields used to store the Beacon objects
    private final String[] beaconIds = {"J8Afaf", "nsk4UG", "5tXSCU", "S3aP63", "zPtPxR"};
    private final String[] beaconUuids = {"D5:00:25:D5:22:A9",
            "FF:48:85:91:B0:0D",
            "E7:4E:95:C8:62:A3",
            "DE:38:78:85:1C:6D",
            "D1:07:0C:8F:45:90"};
    private Beacon[] beaconArray;
    private Map<String /*Beacon UUID*/, Beacon> beaconMap;

    // Used to store Rect objects that will be used to draw a grid to the canvas
    private ArrayList<Rect> grid;

    // Fields for drawing to canvas
    final int QUADRANT_LENGTH = 300;
    final int QUADRANT_STROKE_SIZE = 5;
    private final int GRID_NUMBER_MARGIN = 40;
    private final int GRID_TEXT_MARGIN = 40;
    private final int CURRENT_POINT_HORIZONTAL_MARGIN = 150;
    private final int CURRENT_POINT_VERTICAL_MARGIN = 100;
    private final int LABEL_MARGIN = 100;

    //Paint objects to draw grids, text, and current position
    Paint gridPaint;
    Paint textPaint;
    Paint positionPaint;

    public NavigatorView(Context context) {
        super(context);

        init();

        scanFilters = new ArrayList<>();
        scanFilters.add(new ScanFilter.Builder().setServiceUuid(EDDYSTONE_SERVICE_UUID).build());
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord == null) {
                    Log.d("Scan Record", "null");
                    return;
                }

                // Set beacon as active
                beaconMap.get(result.getDevice().getAddress()).setActive(true);

                // Update Rssi of beacon
                beaconMap.get(result.getDevice().getAddress()).setRssi(result.getRssi());
            }

            @Override
            public void onScanFailed(int errorCode) {
                switch (errorCode) {
                    case SCAN_FAILED_ALREADY_STARTED:
                        logErrorAndShowToast("SCAN_FAILED_ALREADY_STARTED");
                        break;
                    case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                        logErrorAndShowToast("SCAN_FAILED_APPLICATION_REGISTRATION_FAILED");
                        break;
                    case SCAN_FAILED_FEATURE_UNSUPPORTED:
                        logErrorAndShowToast("SCAN_FAILED_FEATURE_UNSUPPORTED");
                        break;
                    case SCAN_FAILED_INTERNAL_ERROR:
                        logErrorAndShowToast("SCAN_FAILED_INTERNAL_ERROR");
                        break;
                    default:
                        logErrorAndShowToast("Scan failed, unknown error code");
                        break;
                }
            }
        }; // End ScanCallBack anonymous inner class

        if (scanner != null) {
            scanner.startScan(scanFilters, SCAN_SETTINGS, scanCallback);
        }

    } // End constructor


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawGrid(canvas);

        if (highestRssiMethod) {
            drawHighestRssiMethod(canvas);
        }
        else if (density1Method) {
            drawDensity1Method(canvas);
        }
        else if (density2Method) {
            drawDensity2Method(canvas);
        }
        else if (bayesMethod) {
            drawBayesMethod(canvas);
        }
        else {
            canvas.drawText("No method selected", LABEL_MARGIN, LABEL_MARGIN, textPaint);
        }


        invalidate();
    }

    // Method 1 uses the current highest RSSI value to calculate the current position of the device
    private void drawHighestRssiMethod(Canvas canvas) {

        // Get the highest Rssi
        highestRssi = -100;
        for (int i = 0; i < beaconUuids.length; i++) {
            if (beaconMap.get(beaconUuids[i]).getRssi() > highestRssi) {
                highestRssi = beaconMap.get(beaconUuids[i]).getRssi();
                currentPosition = beaconMap.get(beaconUuids[i]).getQuadrant();
            }
        }

        // Draw current position onto canvas
        drawPosition(canvas);

        // Label for current method in use
        canvas.drawText("Highest RSSI Method", LABEL_MARGIN, LABEL_MARGIN, textPaint);
    }

    // Density1Method updates sample every time the canvas is redrawn
    private void drawDensity1Method(Canvas canvas) {

        currentPosition = addBeaconToSample();

        // Draw current position onto canvas
        drawPosition(canvas);

        // Label for current method in use
        canvas.drawText("Density1 Method", LABEL_MARGIN, LABEL_MARGIN - 50, textPaint);

        // USED FOR DEBUGGING
        for (int i = 0; i < beaconMap.size(); i++) {
            canvas.drawText("Beacon " + (i + 1) + beaconMap.get(beaconUuids[i]).toString(),
                    LABEL_MARGIN, LABEL_MARGIN + (40 * i), textPaint);
        }
    }

    public void drawDensity2Method(Canvas canvas) {

        elaspedTime = System.currentTimeMillis() - startTime;

        if (elaspedTime >= TIME_INTERVAL) {
            startTime = System.currentTimeMillis();

            currentPosition = addBeaconToSample();
        }

        // Draw current position onto canvas
        drawPosition(canvas);

        // Label for current method in use
        canvas.drawText("Density2 Method", LABEL_MARGIN, LABEL_MARGIN - 50, textPaint);

        // DEBUG
        for (int i = 0; i < beaconMap.size(); i++) {
            canvas.drawText("Beacon " + (i + 1) + beaconMap.get(beaconUuids[i]).toString(),
                    LABEL_MARGIN, LABEL_MARGIN + (40 * i), textPaint);
        }

    }

    // TODO: this method can work with a density algorithm or a highest rssi algorithm
    public void drawBayesMethod(Canvas canvas) {

        BeaconNetwork beaconNetwork = new BeaconNetwork();
        beaconNetwork.initializeNetwork(beaconArray);
        boolean validPosition = false;

        // Highest Rssi implementation
        //*****************************************
        if (currentBeacon == null) {
            previousBeacon = currentBeacon = getHighestRssiBeacon();
            validPosition = true;
        }
        else {
            previousBeacon = currentBeacon;
            currentBeacon = getHighestRssiBeacon();

            // If in same position, then position is valid
            if (currentBeacon.equals(previousBeacon)) {
                validPosition = true;
            }

            Beacon[] validBeacons = beaconNetwork.getNeighborBeacons(previousBeacon);

            for (Beacon beacon : validBeacons) {
                if (beacon == currentBeacon) {
                    validPosition = true;
                }
            }

        }

        if (validPosition && currentBeacon != null) {
            currentPosition = currentBeacon.getQuadrant();
            drawPosition(canvas);
        }
        //*****************************************

        // DEBUG
        if (previousBeacon != null) {
            canvas.drawText("previous beacon: " + previousBeacon.getId(),
                    LABEL_MARGIN, LABEL_MARGIN, textPaint);
        }
        else {
            canvas.drawText("previous beacon: null",
                    LABEL_MARGIN, LABEL_MARGIN, textPaint);
        }

        if (currentBeacon != null) {
            canvas.drawText("current beacon: " + currentBeacon.getId(),
                    LABEL_MARGIN, LABEL_MARGIN + 40, textPaint);
        }
        else {
            canvas.drawText("current beacon: null",
                    LABEL_MARGIN, LABEL_MARGIN + 40, textPaint);
        }



    }

    private void drawPosition(Canvas canvas) {
        float x = grid.get(currentPosition - 1).left + CURRENT_POINT_HORIZONTAL_MARGIN;
        float y = grid.get(currentPosition - 1).top + CURRENT_POINT_VERTICAL_MARGIN;
        canvas.drawPoint(x, y, positionPaint);
    }


    // drawGrid is used to draw the grid to the canvas
    private void drawGrid(Canvas canvas) {
        for (int i = 0; i < grid.size(); i++) {

            // QUADRANT INFO

            final Rect rect = grid.get(i);

            // bounds of each quadrant
            canvas.drawRect(rect, gridPaint);

            // quadrant number in top left
            canvas.drawText(String.valueOf(i + 1), rect.left + GRID_NUMBER_MARGIN,
                    rect.top + GRID_NUMBER_MARGIN, textPaint);

            // BEACON INFO

            final Beacon beacon = beaconMap.get(beaconUuids[i]);

            // beacon UUID
            canvas.drawText(beacon.getUuid(), rect.left + GRID_TEXT_MARGIN, rect.bottom - GRID_TEXT_MARGIN, textPaint);

            // beacon ID
            canvas.drawText(beacon.getId(), rect.left + GRID_TEXT_MARGIN, rect.bottom - (GRID_TEXT_MARGIN * 2), textPaint);

            // RSSI
            canvas.drawText("RSSI: " + String.valueOf(beacon.getRssi()),
                    rect.left + GRID_TEXT_MARGIN, rect.bottom - (GRID_TEXT_MARGIN * 3), textPaint);

        }
    }

    // onSizeChanged is used to set up the bounds for the grid
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // variables used to store bounds for Rect objects
        int left;
        int top;
        int right;
        int bottom;

        // rect1 bounds; used as a reference point to calculate other rect bounds
        left = (int) ((w / 2) - (QUADRANT_LENGTH * 1.5) - QUADRANT_STROKE_SIZE);
        top = QUADRANT_LENGTH;
        right = (int) ((w / 2) - (0.5 * QUADRANT_LENGTH));
        bottom = QUADRANT_LENGTH * 2;
        grid.add(new Rect(left, top, right, bottom));

        // rect2 bounds; top and bottom are the same for rect1, rect2, and rect3
        left = right + QUADRANT_STROKE_SIZE;
        right = left + QUADRANT_LENGTH;
        grid.add(new Rect(left, top, right, bottom));

        // rect3 bounds; top and bottom are the same for rect1, rect2, and rect3
        left = right + QUADRANT_STROKE_SIZE;
        right = left + QUADRANT_LENGTH;
        grid.add(new Rect(left, top, right, bottom));

        // rect4 bounds
        right = left - QUADRANT_STROKE_SIZE;
        left = right - QUADRANT_LENGTH;
        top = bottom + QUADRANT_STROKE_SIZE;
        bottom = top + QUADRANT_LENGTH;
        grid.add(new Rect(left, top, right, bottom));

        // rect5 bounds
        top = bottom + QUADRANT_STROKE_SIZE;
        bottom = top + QUADRANT_LENGTH;
        grid.add(new Rect(left, top, right, bottom));

    }

    private int addBeaconToSample() {

        // Initialize position to default of 1
        int position = 1;

        Beacon newBeacon;

        // Get the beacon with the highest Rssi
        highestRssi = -100; // default highestRssi
        newBeacon = beaconMap.get(beaconUuids[0]); // default newBeacon
        for (int i = 0; i < beaconUuids.length; i++) {
            if (beaconMap.get(beaconUuids[i]).getRssi() > highestRssi) {
                highestRssi = beaconMap.get(beaconUuids[i]).getRssi();
                newBeacon = beaconMap.get(beaconUuids[i]);
            }
        }

        // Increment sample count of beacon
        newBeacon.incrementSampleCount();

        // dequeue oldest beacon and enqueue new beacon
        if (sample.size() == SAMPLE_SIZE) {

            // Dequeue oldest beacon and decrement sample count of beacon
            sample.remove().decrementSampleCount();
            sample.add(newBeacon);
        }
        else { // sample.size() < SAMPLE_SIZE
            sample.add(newBeacon);
        }

        // Get beacon with largest sample count and set current position to beacon's quadrant
        largestSampleCount = 0;
        for (int i = 0; i < beaconUuids.length; i++) {
            if (beaconMap.get(beaconUuids[i]).getSampleCount() > largestSampleCount) {
                largestSampleCount = beaconMap.get(beaconUuids[i]).getSampleCount();
                position = beaconMap.get(beaconUuids[i]).getQuadrant();
            }
        }

        return position;
    }

    public void onResume() {
        // Update booleans
        highestRssiMethod = sharedPreferences.getBoolean(
                getContext().getString(R.string.highestRssiKey), false);
        density1Method = sharedPreferences.getBoolean(
                getContext().getString(R.string.density1Key), false);
        density2Method = sharedPreferences.getBoolean(
                getContext().getString(R.string.density2Key), false);
        bayesMethod = sharedPreferences.getBoolean(
                getContext().getString(R.string.bayesKey), false);

        // initialize startTime
        startTime = System.currentTimeMillis();
    }

    private void logErrorAndShowToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        Log.e(TAG, message);
    }

    // Returns the beacon with the highest Rssi; returns null if no beacons intercepted
    private Beacon getHighestRssiBeacon() {

        // result points to first beacon by default
        Beacon result = beaconMap.get(beaconUuids[0]);

        highestRssi = -100;
        for (int i = 0; i < beaconUuids.length; i++) {
            if (beaconMap.get(beaconUuids[i]).getRssi() > highestRssi) {
                highestRssi = beaconMap.get(beaconUuids[i]).getRssi();
                result = beaconMap.get(beaconUuids[i]);
            }
        }

        if (result.isActive()) {
            return result;
        }

        // return null if resulting beacon is not null
        return null;
    }

    private void init() {

        // Setting up objects to use with the BLE scanner
        BluetoothManager manager = (BluetoothManager) getContext()
                .getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = manager.getAdapter();
        if (btAdapter != null) {
            scanner = btAdapter.getBluetoothLeScanner();
        }

        sharedPreferences = getContext().getSharedPreferences(
                getContext().getString(R.string.pref_file_key), Context.MODE_PRIVATE);

        // grid has not yet been drawn to canvas
        gridDrawn = false;

        // Setup beacon array
        beaconArray = new Beacon[beaconUuids.length];
        for (int i = 0; i < beaconIds.length; i++) {
            beaconArray[i] = new Beacon(i + 1 /* Quadrant */,
                    beaconIds[i], beaconUuids[i]);
        }

        //Initialize HashMap with Beacon objects
        beaconMap = new HashMap<>();
        for (int i = 0; i < beaconArray.length; i++) {
            beaconMap.put(beaconUuids[i], beaconArray[i]);
        }

        // instantiate grid to hold Rect objects
        grid = new ArrayList<>();

        // Instantiate sample
        sample = new LinkedList<>();

        // Initial current position is quadrant 1
        currentPosition = 1;

        // Initial previous position is -1
        previousPosition = -1;

        // Setting up Paint objects
        gridPaint = new Paint();
        gridPaint.setStrokeWidth(QUADRANT_STROKE_SIZE);
        gridPaint.setColor(Color.BLUE);
        gridPaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint();
        textPaint.setTextSize(30);
        textPaint.setColor(Color.BLACK);

        positionPaint = new Paint();
        positionPaint.setStrokeWidth(50);
        positionPaint.setColor(Color.BLACK);

    }

}
