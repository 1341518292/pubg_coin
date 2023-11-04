package com.weian.pubg.config;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.weian.pubg.enums.ResultCodeEnum;
import com.weian.pubg.model.PubgResult;
import com.weian.pubg.model.QuartzEntity;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author Weian
 * @date 2023/10/17 9:37
 */
@Slf4j
@Component
public class QuartzDynamicJobConfig extends QuartzJobBean {

    public static String url = "https://cafe.playbattlegrounds.com/act/a20231012pubg/get_gift";

    @Value("${pubg.ticket}")
    private String ticket;

    @Value("${pubg.max}")
    private Integer max;

    @Value("${pubg.delay}")
    private Integer delay;


    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext)  {
        JobDataMap map = jobExecutionContext.getMergedJobDataMap();
        QuartzEntity entity = (QuartzEntity) map.get("info");
        List<QuartzEntity> quartzEntityList = entity.getQuartzEntityList();
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (int i1 = 0; i1 < quartzEntityList.size(); i1++) {
            QuartzEntity entity1 = quartzEntityList.get(i1);
            task(entity1);
        }

    }



    private PubgResult request(HashMap<String, Object> paramMap) {
        try {
            String json = HttpRequest.post(url)
                    .header(Header.COOKIE, "ticket=" + ticket)
                    .form(paramMap)
                    .timeout(50000)
                    .execute().body();
            return JSONUtil.toBean(json, PubgResult.class);
        }catch (Exception e){
            PubgResult pubgResult = new PubgResult();
            pubgResult.setMsg("请求超时");
            return pubgResult;
        }
    }

    private static Integer i = 0;
    /**
     * 规则锁对象
     */
    private final Lock ruleLock = new ReentrantLock();

    public boolean getRuleState(ThreadPoolExecutor threadPoolExecutor) {
        ruleLock.lock();// 得到锁
        try {
            if( i >= max){
                log.info("超过{}次自动停止", max);
                threadPoolExecutor.shutdownNow();
                threadPoolExecutor.shutdown();
                return true;
            }else {
                return false;
            }
        } finally {
            ruleLock.unlock();// 释放锁
        }
    }

    @SneakyThrows
    private void task(QuartzEntity entity) {
        i = 0;
        HashMap<String, Object> paramMap = new HashMap<>(1);
        String giftId = entity.getGiftId();
        paramMap.put("gift_id", giftId);
        log.info("开始请求:{}", giftId);
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(20, 20,
                30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.DiscardOldestPolicy());
        while (!threadPoolExecutor.isShutdown()){
            threadPoolExecutor.execute(() -> {
                if(getRuleState(threadPoolExecutor)){
                    return;
                }
                PubgResult request = request(paramMap);
                i++;
                log.info("第{}次请求,礼物:{},代码:{},消息:{}",i, giftId,request.getR(),request.getMsg());
                if(ResultCodeEnum.SUCCESS.getValue().equals(request.getR()) ){
                    log.info("搞定,小洞这下知道我的厉害了吧");
                    threadPoolExecutor.shutdownNow();
                }

            });
        }
        Thread.sleep(1000);
        log.info("任务结束!!!");
    }


}
