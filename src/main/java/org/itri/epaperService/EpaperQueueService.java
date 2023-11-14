package org.itri.epaperService;

import org.itri.epaper.Epaper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.itri.epaper.Epaper.BASE_ADDRESS;


@Service
public class EpaperQueueService{

    public static final Queue<Epaper> ePaperQueue = new ConcurrentLinkedQueue<>();
    private Set<String> processingIps = ConcurrentHashMap.newKeySet();

    @Autowired
    private EpaperAsyncService asyncService;

    @Scheduled(fixedRate = 1000)
    public void processEpaper() {
        if (!ePaperQueue.isEmpty()) {
            Epaper epaper = ePaperQueue.poll();

            if(processingIps.contains(epaper.getIpNumber())){
                ePaperQueue.add(epaper);
                return;
            }

            if (!epaper.diffTimestamp()) {
                ePaperQueue.add(epaper);
            } else {
                // 電子紙更新前紀錄於processingIps
                processingIps.add(epaper.getIpNumber());
                // 進行電子紙更新
                CompletableFuture<Boolean> future = asyncService.processEpaperAsync(epaper);
                // 處理電子紙更新結果
                future.thenAccept(success -> {
                    if(success){
                        System.out.println("電子紙更新成功： " + BASE_ADDRESS + epaper.getIpNumber());
                    } else{
                        System.out.println("電子紙更新失敗： " + BASE_ADDRESS + epaper.getIpNumber() +" 重新加回隊列處理。");
                        ePaperQueue.add(epaper);
                    }
                    // 處理完成後，從集合中移除 IP
                    processingIps.remove(epaper.getIpNumber());
                });
            }
        }
    }
}
