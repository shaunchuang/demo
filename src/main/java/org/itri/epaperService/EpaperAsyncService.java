package org.itri.epaperService;


import org.itri.epaper.Epaper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

import static org.itri.epaperService.EpaperQueueService.ePaperQueue;

@Service
public class EpaperAsyncService {

    @Async
    public CompletableFuture<Boolean> processEpaperAsync(Epaper epaper) {
        // 在這裡進行異步處理
        // 例如，對 epaper 進行處理
        boolean result = epaper.updateEpaper();

        if(!result){
            epaper.setNowTimestamp();
            ePaperQueue.add(epaper);
        }

        // 使用 CompletableFuture 完成並返回結果
        return CompletableFuture.completedFuture(result);
    }
}
