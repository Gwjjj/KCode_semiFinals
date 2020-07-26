package com.kuaishou.utils;


public class IpCastLong {

    public static String LongToCallIp(Long ips){
        StringBuilder sb = new StringBuilder();
        Long L = 255L<<56;
        sb.append(((ips&L)>>>56));
        L >>>= 8;
        sb.append(".");
        sb.append(((ips&L)>>>48));
        L >>>= 8;
        sb.append(".");
        sb.append(((ips&L)>>>40));
        L >>>= 8;
        sb.append(".");
        sb.append(((ips&L)>>>32));
        return sb.toString();
    }
    public static String LongToCalledIp(Long ips){
        StringBuilder sb = new StringBuilder();
        Long L = 255L<<56;
        L = L>>>32;
        sb.append(((ips&L)>>>24));
        L >>>= 8;
        sb.append(".");
        sb.append(((ips&L)>>>16));
        L >>>= 8;
        sb.append(".");
        sb.append(((ips&L)>>>8));
        L >>>= 8;
        sb.append(".");
        sb.append(((ips&L)>>>0));
        return sb.toString();
    }
}
