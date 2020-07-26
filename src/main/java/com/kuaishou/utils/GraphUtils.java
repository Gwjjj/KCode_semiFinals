package com.kuaishou.utils;

import com.kuaishou.domain.MyPoint;
import com.kuaishou.kcode.KcodeAlertAnalysisImpl;

import java.util.*;
/*
 * @Description          找最长路径
 * @author Gwjjj
 * @date 10:51 2020/7/26
 */
public class GraphUtils {


    public static List<List<MyPoint>> findLongest(String callName, String calledName) {
        MyPoint from = KcodeAlertAnalysisImpl.graphMap.get(callName);
        MyPoint to = KcodeAlertAnalysisImpl.graphMap.get(calledName);
        List<List<MyPoint>> fromList = findFrom(from);
        List<List<MyPoint>> toList = findTo(to);
        List<List<MyPoint>> reList = new ArrayList<>();
        List<MyPoint> now3;
        for (int i = 0; i < fromList.size(); i++) {
            for (int j = 0; j < toList.size(); j++) {
                now3 = new ArrayList<>();
                now3.addAll(fromList.get(i));
                now3.addAll(toList.get(j));
                reList.add(now3);
            }
        }
        return reList;
    }


    private static List<List<MyPoint>> findFrom(MyPoint point) {
        synchronized (point.from) {                 //  只要有一个线程计算该节点的最长from和to就可以了
            if (point.longFrom != null) {
                return point.longFrom;
            } else {
                int maxLength = 0;
                List<List<MyPoint>> nowPoint;
                List<List<MyPoint>> reList = null;
                int len;
                for (int i = 0; i < point.from.size(); i++) {
                    nowPoint = findFrom(point.from.get(i));
                    if ((len = nowPoint.get(0).size()) > maxLength) {
                        reList = new ArrayList<>();
                        for (int j = 0; j < nowPoint.size(); j++) {
                            List arrayDeque = new ArrayList();
                            arrayDeque.addAll(nowPoint.get(j));
                            reList.add(arrayDeque);
                        }
                        maxLength = len;
                    } else if (len == maxLength) {
                        for (int j = 0; j < nowPoint.size(); j++) {
                            ArrayList<MyPoint> now = new ArrayList<>();
                            now.addAll(nowPoint.get(j));
                            reList.add(now);
                        }
                    }
                }
                if (reList == null) {
                    reList = new ArrayList<>();
                    List de = new ArrayList();
                    de.add(point);
                    reList.add(de);
                    point.longFrom = reList;
                    return reList;
                }
                for (int i = 0; i < reList.size(); i++) {
                    reList.get(i).add(point);
                }
                point.longFrom = reList;
                return reList;
            }
        }
    }

    private static List<List<MyPoint>> findTo(MyPoint point) {
        synchronized (point.to) {
            if (point.longTo != null) {
                return point.longTo;
            } else {
                int maxLength = 0;
                List<List<MyPoint>> nowPoint;
                List<List<MyPoint>> reList = null;
                int len;
                for (int i = 0; i < point.to.size(); i++) {
                    nowPoint = findTo(point.to.get(i));
                    if ((len = nowPoint.get(0).size()) > maxLength) {
                        reList = new ArrayList<>();
                        for (int j = 0; j < nowPoint.size(); j++) {
                            List arrayDeque = new ArrayList();
                            arrayDeque.addAll(nowPoint.get(j));
                            reList.add(arrayDeque);
                        }
                        maxLength = len;
                    } else if (len == maxLength) {
                        for (int j = 0; j < nowPoint.size(); j++) {
                            ArrayList<MyPoint> now = new ArrayList<>();
                            now.addAll(nowPoint.get(j));
                            reList.add(now);
                        }
                    }
                }
                if (reList == null) {
                    reList = new ArrayList<>();
                    List de = new ArrayList();
                    de.add(point);
                    reList.add(de);
                    point.longTo = reList;
                    return reList;
                }
                List<List<MyPoint>> reList2 = new ArrayList<>();
                for (int i = 0; i < reList.size(); i++) {
                    List<MyPoint> list = new ArrayList<>();
                    list.add(point);
                    list.addAll(reList.get(i));
                    reList2.add(list);
                }
                point.longTo = reList2;
                return reList2;
            }
        }
    }
}