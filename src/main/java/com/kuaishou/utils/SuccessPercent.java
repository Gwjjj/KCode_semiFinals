package com.kuaishou.utils;

import java.math.RoundingMode;
import java.text.NumberFormat;

public class SuccessPercent {
    static NumberFormat nf;
//    public static DecimalFormat df = new DecimalFormat("#.00");
    public static final String UNSUCESSPER = "-1%";
    static {
        nf = NumberFormat.getPercentInstance();
        // 保留两位小数
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        // 不需要四舍五入使用RoundingMode.DOWN
        nf.setRoundingMode(RoundingMode.DOWN);
    }

    public static String calSuccessPercent(int successNum,int allNum){
        if(successNum == 0){
            return ".00%";
        }
        double d = (double) successNum/allNum;
        String str = nf.format(d);
        return str;
    }
    public static String calSuccessPercent(double d){
        if(d == 0){
            return ".00%";
        }
        String str = nf.format(d);
        return str;
    }
    public static double castSuccessPercent(String precent){
        if(precent == "-1.00%"){
            return 0;
        }
        precent = precent.substring(0,precent.length()-1);
        return Double.valueOf(precent)/100;
    }
}
