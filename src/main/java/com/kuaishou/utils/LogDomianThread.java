package com.kuaishou.utils;

import com.kuaishou.domain.LogDomain;
import com.kuaishou.domain.MonitorLogDomian;
import com.kuaishou.domain.MyMonitor;
import com.kuaishou.domain.MyPoint;
import com.kuaishou.kcode.KcodeAlertAnalysisImpl;

import java.util.*;

import static com.kuaishou.kcode.KcodeAlertAnalysisImpl.endTime;

public class LogDomianThread implements Runnable{

    private MonitorLogDomian mrLogDomian;
    private String callName;
    private String calledName;

    public LogDomianThread(MonitorLogDomian mrLogDomian, String callName, String calledName) {
        this.mrLogDomian = mrLogDomian;
        this.callName = callName;
        this.calledName = calledName;
    }

    @Override
    public void run() {
        LogDomain[] logDomains;
        List<MyMonitor> monitorList;
        long ips;
        for (Map.Entry<Long, LogDomain[]> entry: mrLogDomian.logMap.entrySet()) {
            ips = entry.getKey();
            logDomains = entry.getValue();
            for (int i = 0; i < endTime; i++) {
                dealLogDomain(logDomains[i]);
            }
            monitorList = mrLogDomian.monitorList;
            for (int j = 0; j < monitorList.size(); j++) {
                addMonitorList(logDomains,monitorList.get(j),ips);
            }
        }
        mrLogDomian.logMap = null;
        mrLogDomian.monitorList = null;

    }


    private void dealLogDomain(LogDomain logDomain) {
        int count;
        if((count = logDomain.allCount.get()) != 0){
            logDomain.successPercent =  (double)logDomain.trueCount.get() / count ;
            List<Integer> timeList = logDomain.timeList;
            MinHeap minHeap = new MinHeap((int) Math.floor(timeList.size() * 0.01) + 1);
            for (int i = 0; i < timeList.size(); i++) {
                minHeap.addNode(timeList.get(i));
            }
            logDomain.p99 =  minHeap.heaps[0];
            logDomain.timeList = null;
        }
    }
    private void addMonitorList(LogDomain[] logDomains, MyMonitor monitor, Long ips){
        int minute = monitor.minute;
        if(endTime < minute)
            return;
        StringBuilder sb;
        Deque<Integer> deque = new ArrayDeque();
        int count = 0;
        int i = 0;
        if(monitor.category){    // P99
            for (; i < minute; i++) {
                if(logDomains[i].p99 > monitor.threshold){
                    count++;
                    deque.addLast(1);
                }
                else {
                    deque.addLast(0);
                }
            }
            if(count == minute){
                q2(i,true);
                sb = new StringBuilder(200);
                sb.append(monitor.index);
                sb.append(",");
                sb.append(TimeCastString.TimeString((long)(KcodeAlertAnalysisImpl.startTime+i)));
                sb.append(",");
                sb.append(callName);
                sb.append(",");
                sb.append(IpCastLong.LongToCallIp(ips));
                sb.append(",");
                sb.append(calledName);
                sb.append(",");
                sb.append(IpCastLong.LongToCalledIp(ips));
                sb.append(",");
                sb.append(logDomains[i].p99);
                sb.append("ms");
                KcodeAlertAnalysisImpl.alarmList.add(sb.toString());
            }
            for (; i < endTime; i++) {
                count -= deque.pollFirst();
                if(logDomains[i].p99 > monitor.threshold){
                    count++;
                    deque.addLast(1);
                }
                else {
                    deque.addLast(0);
                }
                if(count == minute){
                    q2(i,true);
                    sb = new StringBuilder(200);
                    sb.append(monitor.index);
                    sb.append(",");
                    sb.append(TimeCastString.TimeString((long)(KcodeAlertAnalysisImpl.startTime+i)));
                    sb.append(",");
                    sb.append(callName);
                    sb.append(",");
                    sb.append(IpCastLong.LongToCallIp(ips));
                    sb.append(",");
                    sb.append(calledName);
                    sb.append(",");
                    sb.append(IpCastLong.LongToCalledIp(ips));
                    sb.append(",");
                    sb.append(logDomains[i].p99);
                    sb.append("ms");
                    KcodeAlertAnalysisImpl.alarmList.add(sb.toString());
                }
            }
        }
        else {
            for (; i < minute; i++) {
                if(logDomains[i].successPercent < monitor.threshold){
                    count++;
                    deque.addLast(1);
                }
                else {
                    deque.addLast(0);
                }
            }
            if(count == minute){
                q2(i,false);
                sb = new StringBuilder(200);
                sb.append(monitor.index);
                sb.append(",");
                sb.append(TimeCastString.TimeString((long)(KcodeAlertAnalysisImpl.startTime+i)));
                sb.append(",");
                sb.append(callName);
                sb.append(",");
                sb.append(IpCastLong.LongToCallIp(ips));
                sb.append(",");
                sb.append(calledName);
                sb.append(",");
                sb.append(IpCastLong.LongToCalledIp(ips));
                sb.append(",");
                sb.append(logDomains[i].successPercent);
                KcodeAlertAnalysisImpl.alarmList.add(sb.toString());
            }
            for (; i < endTime; i++) {
                count -= deque.pollFirst();
                if(logDomains[i].successPercent < monitor.threshold){
                    count++;
                    deque.addLast(1);
                }
                else {
                    deque.addLast(0);
                }
                if(count == minute){
                    q2(i,false);
                    sb = new StringBuilder(200);
                    sb.append(monitor.index);
                    sb.append(",");
                    sb.append(TimeCastString.TimeString((long)(KcodeAlertAnalysisImpl.startTime+i)));
                    sb.append(",");
                    sb.append(callName);
                    sb.append(",");
                    sb.append(IpCastLong.LongToCallIp(ips));
                    sb.append(",");
                    sb.append(calledName);
                    sb.append(",");
                    sb.append(IpCastLong.LongToCalledIp(ips));
                    sb.append(",");
                    sb.append(SuccessPercent.calSuccessPercent(logDomains[i].successPercent));
                    KcodeAlertAnalysisImpl.alarmList.add(sb.toString());
                }
            }
        }
    }


    private void q2(int time,boolean flag){    //
        if(flag){
            if(mrLogDomian.p99String[time] != null){
                return;
            }
            List<List<MyPoint>> longest = GraphUtils.findLongest(callName, calledName);
            List<String> list = new ArrayList<>();
            MonitorLogDomian monitorLogDomian;
            for (int i = 0; i < longest.size(); i++) {
                StringBuilder sb = new StringBuilder(400);
                StringBuilder sb1 = new StringBuilder(128);
                List<MyPoint> myPoints = longest.get(i);
                String nowCalled;
                String preCall = null;
                for (int j = 0;j<myPoints.size();j++) {
                    nowCalled = myPoints.get(j).name;
                    if (preCall != null) {
                        monitorLogDomian = KcodeAlertAnalysisImpl.countMap.get(preCall).get(nowCalled);
                        sb.append("->");
                        synchronized (monitorLogDomian.p99[time]) {
                            if (monitorLogDomian.p99[time].length() == 0) {
                                monitorLogDomian.p99[time] = calP99(monitorLogDomian.allList[time]);
                                monitorLogDomian.allList[time] = null;
                            }
                        }
                        sb1.append(monitorLogDomian.p99[time]);
                        sb1.append("ms,");
                    }
                    sb.append(nowCalled);
                    preCall = nowCalled;
                }
                sb.append("|");
                sb.append(sb1);
                sb.setLength(sb.length()-1);
                list.add(sb.toString());
            }
            mrLogDomian.p99String[time] = list;
        }
        else{
            if(mrLogDomian.spString[time] != null){
                return;
            }
            List<List<MyPoint>> longest = GraphUtils.findLongest(callName, calledName);
            List<String> list = new ArrayList<>();
            MonitorLogDomian monitorLogDomian;
            for (int i = 0; i < longest.size(); i++) {
                StringBuilder sb = new StringBuilder(400);
                StringBuilder sb1 = new StringBuilder(128);
                List<MyPoint> myPoints = longest.get(i);
                String nowCalled;
                String preCall = null;
                for (int j = 0;j<myPoints.size();j++) {
                    nowCalled = myPoints.get(j).name;
                    if (preCall != null) {
                        monitorLogDomian = KcodeAlertAnalysisImpl.countMap.get(preCall).get(nowCalled);
                        sb.append("->");
                        synchronized (monitorLogDomian.sp) {
                            if (monitorLogDomian.sp[time] == null) {
                                monitorLogDomian.sp[time] = SuccessPercent.calSuccessPercent(monitorLogDomian.trueC[time].get(), monitorLogDomian.allC[time].get());
                            }
                        }
                        sb1.append(monitorLogDomian.sp[time]);
                        sb1.append(",");
                    }
                    sb.append(nowCalled);
                    preCall = nowCalled;
                }
                sb.append("|");
                sb.append(sb1);
                sb.setLength(sb.length()-1);
                list.add(sb.toString());
            }
            mrLogDomian.spString[time] = list;
        }
    }


    private String calP99(List<Integer> list){
        if(list.size() == 0){
            return "-1";
        }
        MinHeap minHeap = new MinHeap((int) Math.floor(list.size() * 0.01) + 1);
        for (int i = 0; i < list.size(); i++) {
            minHeap.addNode(list.get(i));
        }
        return String.valueOf(minHeap.heaps[0]);
    }
}
