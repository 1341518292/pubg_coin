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

import java.util.concurrent.ThreadPoolExecutor;


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

    @Autowired
    ThreadPoolExecutor threadPoolExecutor;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext)  {
        JobDataMap map = jobExecutionContext.getMergedJobDataMap();
        QuartzEntity entity = (QuartzEntity) map.get("info");
        task(entity);
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

    private static Integer i = 1;

    @SneakyThrows
    private void task(QuartzEntity entity) {
        HashMap<String, Object> paramMap = new HashMap<>(1);
        String giftId = entity.getGiftId();
        paramMap.put("gift_id", giftId);
        //链式构建请求
        log.info("开始请求:{}", giftId);
        PubgResult pubgResult = request(paramMap);
        log.info(pubgResult.getMsg());
        if (ResultCodeEnum.SUCCESS.getValue().equals(pubgResult.getR())) {
            return;
        }
        log.info("蓝洞搞事情我也开搞！启动暴力请求");
        while (!threadPoolExecutor.isShutdown()){
            threadPoolExecutor.execute(() -> {
                PubgResult request = request(paramMap);
                i++;
                log.info("第{}次请求,礼物:{},代码:{},消息:{}",i, giftId,request.getR(),request.getMsg());
                if(ResultCodeEnum.SUCCESS.getValue().equals(request.getR())){
                    log.info("搞定,小洞这下知道我的厉害了吧");
                    threadPoolExecutor.shutdownNow();
                }
            });
        }
        log.info("任务结束!!!");
    }


}
