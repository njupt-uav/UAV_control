package com.dji.GSDemo.GaodeMap;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMap.OnMapClickListener;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

/**
 * 大疆这群工程师真是够可以的，开源的demo居然没注释，我只能呵呵
 */
public class Waypoint1Activity extends FragmentActivity implements View.OnClickListener, OnMapClickListener {

    protected static final String TAG = "Waypoint1Activity";

    private MapView mapView;
    private AMap aMap;

    private Button locate, add, clear;
    private Button config, upload, start, stop;

    private boolean isAdd = false;

    private double droneLocationLat = 181, droneLocationLng = 181;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;

    private float altitude = 100.0f;
    private float mSpeed = 10.0f;
    //配置基本的变量和参数
    private List<Waypoint> waypointList = new ArrayList<>();

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;

    @Override
    protected void onResume(){
        super.onResume();
        initFlightController();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        unregisterReceiver(mReceiver);
        removeListener();
        super.onDestroy();
    }

    /**
     * @Description : RETURN Button RESPONSE FUNCTION
     */
    public void onReturn(View view){
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void setResultToToast(final String string){
        Waypoint1Activity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Waypoint1Activity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUI() {

        locate = (Button) findViewById(R.id.locate);
        add = (Button) findViewById(R.id.add);
        clear = (Button) findViewById(R.id.clear);
        config = (Button) findViewById(R.id.config);
        upload = (Button) findViewById(R.id.upload);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);

        locate.setOnClickListener(this);
        add.setOnClickListener(this);
        clear.setOnClickListener(this);
        config.setOnClickListener(this);
        upload.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);

    }
    //为UI创建MapView和AMap变量以及7个Button成员变量，创建 initUI() 方法以初始化7个Button变量，并实现它们的 setOnClickListener 方法并将 "this" 作为参数传递。

    private void initMapView() {

        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setOnMapClickListener(this);// add the listener for click for amap object
        }

        LatLng shenzhen = new LatLng(22.5362, 113.9454);
        aMap.addMarker(new MarkerOptions().position(shenzhen).title("Marker in Shenzhen"));
        aMap.moveCamera(CameraUpdateFactory.newLatLng(shenzhen));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_waypoint1);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);

        initMapView();
        initUI();
        addListener();
        onProductConnectionChange();
    }
/*
 在 onCreate() 方法中，我们在运行时请求多个权限，以确保当编译和目标SDK版本高于22
 */
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initFlightController();
        loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        setResultToToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }
    //注册一个新的账户

    private void initFlightController() {

        BaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {

            mFlightController.setStateCallback(
                    new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                            droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                            updateDroneLocation();
                        }
                    });

        }
    }
    //初始化飞行的控制

    //Add Listener for WaypointMissionOperator
    private void addListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }

    private void removeListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
        }
    }


    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {

        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {

        }

        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {

        }

        @Override
        public void onExecutionStart() {

        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
        }
    };

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return instance;
    }

    @Override
    public void onMapClick(LatLng point) {
        if (isAdd == true){
            markWaypoint(point);
            Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);
            //Add Waypoints to Waypoint arraylist;
            if (waypointMissionBuilder != null) {
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }else
            {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        }else{
            setResultToToast("Cannot Add Waypoint");
        }
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }
    /*
    调用 initUI() 方法初始化UI。 调用 initMapView() 方法创建MapView并在此处添加中国深圳的标记。
    因此，加载高德地图时，您将在中国深圳看到一个蓝色别针标签。
     */

    // Update the drone location based on states from MCU.
    private void updateDroneLocation(){

        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = aMap.addMarker(markerOptions);
                }
            }
        });
    }

    private void markWaypoint(LatLng point){
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = aMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }
//创建标记路径点的方法
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.locate:{
                updateDroneLocation();
                cameraUpdate(); // Locate the drone's place
                break;
            }
            case R.id.add:{
                enableDisableAdd();
                break;
            }
            case R.id.clear: {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        aMap.clear();
                    }

                });
                waypointList.clear();
                waypointMissionBuilder.waypointList(waypointList);
                updateDroneLocation();
                break;
            }
            case R.id.config:{
                showSettingDialog();
                break;
            }
            case R.id.upload:{
                uploadWayPointMission();
                break;
            }
            case R.id.start:{
                startWaypointMission();
                break;
            }
            case R.id.stop:{
                stopWaypointMission();
                break;
            }
            default:
                break;
        }
    }
    //标记各个按钮的作用，用case来做比较清晰

    private void cameraUpdate(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        aMap.moveCamera(cu);

    }

    private void enableDisableAdd(){
        if (isAdd == false) {
            isAdd = true;
            add.setText("Exit");
        }else{
            isAdd = false;
            add.setText("Add");
        }
    }

    private void showSettingDialog(){
        LinearLayout wayPointSettings = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);

        final TextView wpAltitude_TV = (TextView) wayPointSettings.findViewById(R.id.altitude);
        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed);
        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);

        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.lowSpeed){
                    mSpeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed){
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed){
                    mSpeed = 10.0f;
                }
            }
            //设定无人机的三种行驶速度的模式

        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select finish action");
                if (checkedId == R.id.finishNone){
                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                } else if (checkedId == R.id.finishGoHome){
                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                } else if (checkedId == R.id.finishAutoLanding){
                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                } else if (checkedId == R.id.finishToFirst){
                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });
        //设定无人机返回的操作

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select heading");

                if (checkedId == R.id.headingNext) {
                    mHeadingMode = WaypointMissionHeadingMode.AUTO;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
            }
        });
        //设定无人机的朝向

        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("Finish",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {

                        String altitudeString = wpAltitude_TV.getText().toString();
                        altitude = Integer.parseInt(nulltoIntegerDefalt(altitudeString));
                        Log.e(TAG,"altitude "+altitude);
                        Log.e(TAG,"speed "+mSpeed);
                        Log.e(TAG, "mFinishedAction "+mFinishedAction);
                        Log.e(TAG, "mHeadingMode "+mHeadingMode);
                        configWayPointMission();
                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                })
                .create()
                .show();
    }

    String nulltoIntegerDefalt(String value){
        if(!isIntValue(value)) value="0";
        return value;
    }

    boolean isIntValue(String val)
    {
        try {
            val=val.replace(" ","");
            Integer.parseInt(val);
        } catch (Exception e) {return false;}
        return true;
    }
//越界报错判断
    private void configWayPointMission(){

        if (waypointMissionBuilder == null){

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                                                                  .headingMode(mHeadingMode)
                                                                  .autoFlightSpeed(mSpeed)
                                                                  .maxFlightSpeed(mSpeed)
                                                                  .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }else
        {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }

        if (waypointMissionBuilder.getWaypointList().size() > 0){

            for (int i=0; i< waypointMissionBuilder.getWaypointList().size(); i++){
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }

            setResultToToast("Set Waypoint attitude successfully");
        }

        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("loadWaypoint succeeded");
        } else {
            setResultToToast("loadWaypoint failed " + error.getDescription());
        }

    }
    //实现showSettingDialog 方法以显示 Waypoint Configuration 警报对话框，并在按下 Config 按钮时重写 onClick() 方法显示配置对话框。

    private void uploadWayPointMission(){

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    setResultToToast("Mission upload successfully!");
                } else {
                    setResultToToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });

    }

    private void startWaypointMission(){

        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }
//创建开始行进的命令方法
    private void stopWaypointMission(){

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }
//创建停止行进的命令方法
}
