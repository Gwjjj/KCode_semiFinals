package com.kuaishou.utils;
import java.util.Calendar;

public class TimeInit {
    static int START_DAY;
    static int START_HOUR_OF_DAY;
    static int START_MINUTE;

    /*
     * @Author Gwjjj
     * @Description             快速转换时间
     * @param time              yyyy-MM-dd HH:mm
     * @Date 17:11 2020/7/26
     * @Return
     **/
    public static int stringCastTime(String time){
        int h = (time.charAt(11)-'0')*10 + (time.charAt(12)-'0');
        int min = (time.charAt(14)-'0')*10+(time.charAt(15)-'0');
        min =   (h - START_HOUR_OF_DAY)*60+ (min-START_MINUTE);
        return min;
    }


    /*
     * @Author Gwjjj
     * @Description                     用开始时间初始化时间类
     * @param time                      long 时间戳
     * @Date 22:49 2020/7/25
     * @Return
     **/
    public static void TimeCastString(Long time){
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(time);
        START_DAY = calendar.get(Calendar.DATE);                        //  天
        START_HOUR_OF_DAY = calendar.get(Calendar.HOUR_OF_DAY);         //  小时
        START_MINUTE = calendar.get(Calendar.MINUTE)-1;                 //  分
    }
}