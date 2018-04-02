package com.fstar.tv.tools;

/**
 * Created by rojama on 2014-12-25.
 */
public class Config {
    //阿里云
    static public String updateUrl = "http://121.40.112.83:8080/APKUPDATE/version.xml";
    static public String serverBaseIP = "121.40.112.83";
    static public String serverBaseUrl = "http://"+serverBaseIP+":8085/FStarWeb";

    //云数通
//    static public String updateUrl = "http://222.76.210.151:8080/APKUPDATE/version.xml";
//    static public String serverBaseUrl = "http://222.76.210.151:8085/FStarWeb";

    //local
//    static public String serverBaseUrl = "http://192.168.1.106:8080/FStarWeb";

    static public String inmageUrlPrefix = serverBaseUrl
            +"/cm?ProcessBO=com.fstar.cms.TVServerImageBO&ProcessMETHOD=getImage";
}
