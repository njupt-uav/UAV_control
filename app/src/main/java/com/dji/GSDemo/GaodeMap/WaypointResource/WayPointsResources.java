package com.dji.GSDemo.GaodeMap.WaypointResource;

import com.amap.api.maps2d.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于存放需要增加的航点
 */
public class WayPointsResources {
    /**
     * 用于存放需要存放的航点,有序
     * 存入的坐标系为GCJ02
     */
    public static List<LatLng> points = new ArrayList<>();

    /**
     * 是否保留手动增加的航点，如果为true，则在已有手动添加的航点之后增加其他航点
     */
    public static boolean whetherRetainExistingWaypoint = true;
    static {
//        LatLng(lat,lag)
        points.add(new LatLng(32.114754,118.928587));
        points.add(new LatLng(32.115052,118.928755));
        points.add(new LatLng(32.114712,118.929070));
    }
}
