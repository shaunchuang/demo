package org.itri.epaperController;

import org.itri.epaper.Epaper;
import org.itri.epaperService.EpaperQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

@RestController
@RequestMapping("/epaper")
public class EpaperController {

    @Autowired
    private EpaperQueueService queueService;

    @PostMapping("/update")
    public ResponseEntity<?> updateEpaper(@RequestBody Epaper epaper) {
        if (epaper.getBase64Image() != null && epaper.getBase64Image() != "" && epaper.getIpNumber() != null && epaper.getIpNumber() != "" ){
            epaper.setNowTimestamp();
            EpaperQueueService.ePaperQueue.add(epaper);
            return ResponseEntity.ok(Boolean.TRUE);
        } else{
            return ResponseEntity.ok(Boolean.FALSE);
        }
    }

    @PostMapping("/updateWithParams")
    public ResponseEntity<?> updateEpaperWithParams(@RequestParam String ipNumber, @RequestParam String imageName) {
        // 验证参数是否有效
        if (ipNumber == null || ipNumber.isEmpty() || imageName == null || imageName.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid IP number or image name");
        }
        // 創建 Epaper 對象並設置值
        Epaper epaper = new Epaper(ipNumber);
        epaper.setIpNumber(ipNumber);
        // 假定您有方法来根据图片名称获取其Base64编码，如 getImageBase64ByName
        String base64Image = getImageBase64ByName(imageName);
        if (base64Image != null) {
            epaper.setBase64Image(base64Image);
            epaper.setNowTimestamp();
            EpaperQueueService.ePaperQueue.add(epaper);
            return ResponseEntity.ok(Boolean.TRUE);
        } else {
            return ResponseEntity.badRequest().body("Image not found");
        }
    }

    private String getImageBase64ByName(String imageName) {
        try {
            // 假設圖片存儲在一個名為 'images' 的目錄中
            File imageFile = new File("D:\\電子紙圖片\\" + imageName);
            if (imageFile.exists()) {
                // 讀取文件內容
                byte[] fileContent = Files.readAllBytes(imageFile.toPath());
                // 將文件內容轉換為 Base64 編碼
                return Base64.getEncoder().encodeToString(fileContent);
            } else {
                // 文件不存在
                return null;
            }
        } catch (IOException e) {
            // 處理讀取文件時的異常
            e.printStackTrace();
            return null;
        }
    }
}
