package org.itri.epaper;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Level;
import javax.imageio.ImageIO;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;


public class Epaper {

    // Epaper static setting
    public static final String BASE_ADDRESS = "192.168.225.";

    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private long timeStamp;

    private long waitTimeStamp = 15;

    private String base64Image;

    private String ipNumber;

    public String getBase64Image() {
        return base64Image;
    }

    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }

    public String getIpNumber() {
        return ipNumber;
    }

    public void setIpNumber(String ipNumber) {
        this.ipNumber = ipNumber;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long waitTime){ this.timeStamp = waitTime; }

    public void setNowTimestamp(){
        this.timeStamp = System.currentTimeMillis();
    }

    public boolean diffTimestamp(){
        if (System.currentTimeMillis() - this.getTimeStamp() > waitTimeStamp) return true;
        else return false;
    }

    public Epaper(String base64Image, String ipNumber) {
        this.base64Image = base64Image;
        this.ipNumber = ipNumber;
    }

    public Epaper(String ipNumber){
        this.ipNumber = ipNumber;
    }


    public boolean updateEpaper() {
        try {

            String ipAddress = BASE_ADDRESS + ipNumber;

//			BufferedImage bufferedimage = image;

//			BufferedImage image = ImageIO.read(new File(imagePath));

            // 檢查圖像尺寸
//            if (image.getWidth() != 400 || image.getHeight() != 300) {
//                log(Level.INFO, "Image dimensions are not 400x300.");
//                return false;
//            }
            byte[] imageByte = Base64.getDecoder().decode(base64Image);

            ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);

            BufferedImage image = ImageIO.read(bis);
            // 轉換圖像
            int[][] binaryImage = convertToBinary(image);

            // 將二進制圖像轉換為字符串
            String result = convertImageToString(binaryImage);

            // 上傳結果給電子紙並更新
            log(Level.INFO, "Upload ESP8266 " + ipAddress);

            if(uploadInBatchesESP8266(ipAddress, result)){
                return true;
            } else{
                return false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, e.toString(), e);
        }
        return false;
    }

    // 批次上傳資料 for ESP32
    /* 目前不需要ESP32更新方式
    public boolean uploadInBatchesESP32(String ipAddress, String data) {
        int BATCH_SIZE = 1000;
        String BASE_URL = "http://" + ipAddress + "/";
        String START_REQUEST = BASE_URL + "EPDI_";
        String NEXT_REQUEST = BASE_URL + "NEXT_";
        String repeatedStr = "ppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppiodaLOAD_";
        String GAP_REQUEST = BASE_URL + repeatedStr;
        String END_REQUEST = BASE_URL + "SHOW_";
        try {

            if (!isHostReachable(ipAddress, 5000, 2)) {
                log(Level.INFO, "電子紙無法連線. 取消作業 " + ipAddress);
                return false;
            }

            if (!sendGetRequest(BASE_URL, null, 2000)) {
                log(Level.INFO, "電子紙無網頁. 取消作業 " + ipAddress);
                return false;
            }

            Thread.sleep(100);
            // 發送起始請求
            if (!sendPostRequest(START_REQUEST, "", 10000)) {
                log(Level.INFO, "電子紙起始請求失敗. 取消作業 " + ipAddress);
                return false;
            }

            // 發送電子紙圖像資訊
            for (int i = 0; i < data.length(); i += BATCH_SIZE) {
                String batch = data.substring(i, Math.min(i + BATCH_SIZE, data.length()));
                String requestUrl = BASE_URL + batch + "iodaLOAD_";
                sendPostRequest(requestUrl, "", 5000);
            }

            // 發送NEXT訊號
            sendPostRequest(NEXT_REQUEST, "", 5000);

            // 發送GAP訊號
            for (int i = 0; i < 30; i++) {
                sendPostRequest(GAP_REQUEST, "", 5000);
            }

            // 發送結束請求
            sendPostRequest(END_REQUEST, "", 27000);
            log(Level.INFO, "電子紙更新完成 " + ipAddress);
        } catch (Exception e) {
            log(Level.SEVERE, e.toString(), e);
            return false;
        }
        return true;
    }

     */

    // 批次上傳資料 ESP8266
    public boolean uploadInBatchesESP8266(String ipAddress, String data) {
        int BATCH_SIZE = 1500;
        String baseUrl = "http://" + ipAddress + "/";
        String repeatedStr = "pppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppp";
        String START_REQUEST = baseUrl + "EPD";
        String NEXT_REQUEST = baseUrl + "NEXT";
        String END_REQUEST = baseUrl + "SHOW";
        try {

            if (!isHostReachable(ipAddress, 5000, 2)) {
                log(Level.INFO, "電子紙無法連線. 取消作業 " + ipAddress);
                return false;
            }


            Thread.sleep(100);
            // 發送起始請求
            // 4.2b v2 為 "cc"，4.2 為 "na"
            if (checkIPRange(ipAddress).equals("w")) {
                if (!sendPostRequest(START_REQUEST, "na", 10000)) {
                    log(Level.INFO, "電子紙起始請求失敗. 取消作業 " + ipAddress);
                    return false;
                }
            } else if (checkIPRange(ipAddress).equals("r")) {
                if (!sendPostRequest(START_REQUEST, "cc", 10000)) {
                    log(Level.INFO, "電子紙起始請求失敗. 取消作業 " + ipAddress);
                    return false;
                }
            } else {
                log(Level.INFO, "目標IP不位於所在範圍內 " + ipAddress);
                return false;
            }

            // 發送電子紙圖像資訊
            for (int i = 0; i < data.length(); i += BATCH_SIZE) {
                String batch = data.substring(i, Math.min(i + BATCH_SIZE, data.length()));
                String payload = batch + "mnfaLOAD";
                sendPostRequest(baseUrl + "LOAD", payload, 5000);
            }

            // 發送NEXT訊號
            sendPostRequest(NEXT_REQUEST, "", 5000);

            // 發送GAP訊號
            for (int i = 0; i < 20; i++) {
                sendPostRequest(baseUrl + "LOAD", repeatedStr + "mnfaLOAD", 5000);
            }

            // 發送結束請求
            sendPostRequest(END_REQUEST, "", 10000);
            log(Level.INFO, "電子紙更新完成 " + ipAddress);
        } catch (Exception e) {
            log(Level.SEVERE, e.toString(), e);
            return false;
        }
        return true;
    }

    private boolean sendPostRequest(String requestUrl, String payload, int timeout) throws Exception {
        CloseableHttpClient httpClient = null;

        try {
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout)
                    .setConnectTimeout(5000).build();

            httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

            log(Level.INFO, "Create connection " + requestUrl);
            HttpPost httpPost = new HttpPost(requestUrl);
            httpPost.setEntity(new StringEntity(payload));

            CloseableHttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                log(Level.INFO, "Response is OK " + requestUrl);
                return true;
            } else {
                log(Level.INFO, "Response is Bad(" + statusCode + ") " + requestUrl);
            }
        } catch (ConnectTimeoutException e) {
            log(Level.INFO, "ConnectTimeoutException :" + requestUrl + " " + e.getMessage());
        } catch (SocketTimeoutException e) {
            log(Level.INFO, "SocketTimeoutException :" + requestUrl + " " + e.getMessage());
        } catch (NoHttpResponseException e) {
            // log(Level.INFO, "NoHttpResponseException : " + e.getMessage());
        } catch (SocketException e) {
            log(Level.INFO, "SocketException : " + requestUrl + " " + e.getMessage());
        } catch (Exception e) {
            log(Level.INFO, "Exception : " + requestUrl + " " + e.getMessage());
        } finally {
            try {
                httpClient.close();
            } catch (Exception ex) {
            }
            httpClient = null;
        }
        return false;
    }

    private boolean sendGetRequest(String requestUrl, String payload, int timeout) throws Exception {
        CloseableHttpClient httpClient = null;
        try {
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout( timeout)
                    .setConnectTimeout(5000).build();
            httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

            log(Level.INFO, "Create connection " + requestUrl);
            HttpGet httpGet = new HttpGet(requestUrl);

            CloseableHttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                log(Level.INFO, "Response is OK " + requestUrl);
                return true;
            } else {
                log(Level.INFO, "Response is Bad(" + statusCode + ") " + requestUrl);
            }
        } catch (ConnectTimeoutException e) {
            log(Level.INFO, "ConnectTimeoutException :" + requestUrl + " " + e.getMessage());
        } catch (SocketTimeoutException e) {
            log(Level.INFO, "SocketTimeoutException :" + requestUrl + " " + e.getMessage());
        } catch (NoHttpResponseException e) {
            // log(Level.INFO, "NoHttpResponseException : " + e.getMessage());
        } catch (SocketException e) {
            log(Level.INFO, "SocketException : " + requestUrl + " " + e.getMessage());
        } catch (Exception e) {
            log(Level.INFO, "Exception : " + requestUrl + " " + e.getMessage());
        } finally {
            try {
                httpClient.close();
            } catch (Exception ex) {
            }
            httpClient = null;
        }
        return false;
    }

    public int[][] convertToBinary(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] result = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = image.getRGB(x, y);
                int red = (color >> 16) & 0xff;
                int green = (color >> 8) & 0xff;
                int blue = color & 0xff;

                // 計算灰階度值
                int gray = (red + green + blue) / 3;

                // 設定二進制值
                result[y][x] = gray < 200 ? 0 : 1;
            }
        }

        return result;
    }

    // 將binary 資訊轉成String
    public String convertImageToString(int[][] binaryImage) {
        StringBuilder sb = new StringBuilder();

        for (int y = 0; y < binaryImage.length; y++) {
            for (int x = 0; x < binaryImage[y].length; x += 8) {
                int value = 0;
                for (int i = 0; i < 8; i++) {
                    value = (value << 1) | binaryImage[y][x + i];
                }
                sb.append(byteToStr(value));
            }
        }

        return sb.toString();
    }

    // 將byte轉換成String
    public String byteToStr(int v) {
        char char1 = (char) ((v & 0xF) + 97);
        char char2 = (char) (((v >> 4) & 0xF) + 97);
        return new String(new char[] { char1, char2 });
    }

    private boolean isHostReachable(String host, int timeout, int retries) {
        for (int i = 0; i < retries; i++) {
            try {
                InetAddress address = InetAddress.getByName(host);
                if (address.isReachable(timeout)) {
                    return true;
                } else {
                    log(Level.INFO, "電子紙無法連線. 2秒後重試 " + host);
                    Thread.sleep(2000);
                }
            } catch (Exception e) {
                log(Level.SEVERE, e.toString(), e);
            }
        }
        return false;
    }

    public boolean isIPInRange(String ip, String startRange, String endRange) {
        try {
            long ipLong = ipToLong(InetAddress.getByName(ip));
            long startRangeLong = ipToLong(InetAddress.getByName(startRange));
            long endRangeLong = ipToLong(InetAddress.getByName(endRange));

            return ipLong >= startRangeLong && ipLong <= endRangeLong;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public long ipToLong(InetAddress ip) {
        byte[] octets = ip.getAddress();
        long result = 0;
        for (byte octet : octets) {
            result <<= 8;
            result |= octet & 0xff;
        }
        return result;
    }

    public String checkIPRange(String ipAddress) {

        if (isIPInRange(ipAddress, "192.168.225.100", "192.168.225.200")) {
            System.out.println("IP is in range 192.168.225.100-200");
            return "w";
        } else
            try {
                if (ipToLong(InetAddress.getByName(ipAddress)) > ipToLong(InetAddress.getByName("192.168.225.200"))) {
                    System.out.println("IP is greater than 192.168.225.200");
                    return "r";
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        return "na";
    }

    public static void log(Level level, String message, Exception e) {
        if (e == null) {
            // logger.log(level, message);
            String timeStamp = dateFormat.format(new Date());
            System.out.println(timeStamp + "\t" + message);
        } else {
            // logger.log(level, message, e);
            e.printStackTrace();
        }

    }

    public static void log(Level level, String message) {
        log(level, message, null);
    }
}
