package com.kuaishou.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TimeCastString {
    private static SimpleDateFormat sim=new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static Lock lock = new ReentrantLock();

    public static String TimeString(Long timeNum){
        Date date = new Date(timeNum*1000*60);
        lock.lock();
        String time=sim.format(date);
        lock.unlock();
        return time;
    }

//    public static int TimeLong(String timeString)  {
//        Date date = null;
//        try {
//            date = sim.parse(timeString);
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//        Long timestamp=date.getTime();
//        return (int)(timestamp/60000);
//    }
}
