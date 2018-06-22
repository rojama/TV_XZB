package com.fstar.tv.tools;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Rojama on 2014/12/20.
 */
public class Utils {
    static public String readHttp(String url) {
        System.out.println("readHttp:" + url);
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } else {
                Log.e(Utils.class.toString(), "Failed to download file");
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    static public JSONObject readHttpJSON(String url) {
        JSONObject json = null;
        String input = readHttp(url);
        try {
            json = new JSONObject(input);
            Log.i(Utils.class.getName(), json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    static public Map readHttpMap(String url) {
        return parserToMap(readHttp(url));
    }

    public static Map parserToMap(String s) {
        Map map = new HashMap();
        try {
            JSONObject json = new JSONObject(s);
            Iterator keys = json.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String value = json.get(key).toString();
                if (value.startsWith("{") && value.endsWith("}")) {
                    map.put(key, parserToMap(value));
                } else if (value.startsWith("[") && value.endsWith("]")) {
                    map.put(key, json.getJSONArray(key));
                } else {
                    map.put(key, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    static public Bitmap parserImage(URL picUrl) throws IOException {
        //检查缓存
        Bitmap bitmap = BitmapFactory.decodeStream(picUrl.openStream());
        return bitmap;
    }

    private ImageMemoryCache memoryCache;
    private ImageFileCache fileCache;

    public Bitmap getBitmap(String url) {
        // 从内存缓存中获取图片
        Bitmap result = memoryCache.getBitmapFromCache(url);
        if (result == null) {
            // 文件缓存中获取
            result = fileCache.getImage(url);
            if (result == null) {
                // 从网络获取
                result = ImageGetFromHttp.downloadBitmap(url);
                if (result != null) {
                    fileCache.saveBitmap(result, url);
                    memoryCache.addBitmapToCache(url, result);
                }
            } else {
                // 添加到内存缓存
                memoryCache.addBitmapToCache(url, result);
            }
        }
        return result;
    }

    private static final char HEX_DIGITS[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);
            sb.append(HEX_DIGITS[b[i] & 0x0f]);
        }
        return sb.toString();
    }

    public static String Bit32(String SourceString) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(SourceString.getBytes());
        byte messageDigest[] = digest.digest();
        return toHexString(messageDigest);
    }

    public static String Bit16(String SourceString) throws Exception {
        return Bit32(SourceString).substring(8, 24);
    }


    public static File downLoadFile(String httpUrl) throws Exception {

        String fileName = "tv-updata.apk";
        File tmpFile = new File("/sdcard/update");
        if (!tmpFile.exists()) {
            tmpFile.mkdir();
        }
        final File file = new File("/sdcard/update/" + fileName);

        URL url = new URL(httpUrl);
        HttpURLConnection conn = (HttpURLConnection) url
                .openConnection();
        InputStream is = conn.getInputStream();
        FileOutputStream fos = new FileOutputStream(file);
        byte[] buf = new byte[256];
        conn.connect();
        double count = 0;
        if (conn.getResponseCode() >= 400) {
//                    Toast.makeText(Main.this, "连接超时", Toast.LENGTH_SHORT)
//                            .show();
            return null;
        } else {
            while (count <= 100) {
                if (is != null) {
                    int numRead = is.read(buf);
                    if (numRead <= 0) {
                        break;
                    } else {
                        fos.write(buf, 0, numRead);
                    }

                } else {
                    break;
                }

            }
        }

        conn.disconnect();
        fos.close();
        is.close();


        return file;
    }

    public static boolean isIpReachable(String ip)
    {
        try
        {
            InetAddress addr = InetAddress.getByName(ip);
            if (addr.isReachable(3000))
            {
                return true;
            }
        }
        catch (Exception e)
        {
        }
        return false;
    }

    public static String timeToString(int timeMs) {
        StringBuilder mFormatBuilder;
        Formatter mFormatter;
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d时%02d分%02d秒", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d分%02d秒", minutes, seconds).toString();
        }
    }


}
