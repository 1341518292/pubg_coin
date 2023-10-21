package com.weian.pubg.service;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.weian.pubg.enums.ResultCodeEnum;
import com.weian.pubg.model.PubgResult;
import com.weian.pubg.model.QuartzEntity;
import com.weian.pubg.util.QuartzUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.HashMap;

import static com.weian.pubg.config.QuartzDynamicJobConfig.url;

/**
 * @author Weian
 * @date 2023/10/17 9:33
 */
@Component
@Slf4j
public class jobService {

    @Autowired
    QuartzUtil quartzUtil;

    @Value("${pubg.cron}")
    private String cron;
    @Value("${pubg.silverChest}")
    private Boolean silverChest;

    @Value("${pubg.bronzeChest}")
    private Boolean bronzeChest;

    @Value("${pubg.ticket}")
    private String ticket;

    @SneakyThrows
    @PostConstruct
    public void init(){
        if(silverChest){
            log.info("添加白银宝箱定时");
            QuartzEntity quartzEntity = new QuartzEntity("1", cron, "300005");
            quartzUtil.addScheduler(quartzEntity);
        }
        if(bronzeChest){
            log.info("添加青铜宝箱定时");
            QuartzEntity quartzEntity1 = new QuartzEntity("2", cron, "300006");
            quartzUtil.addScheduler(quartzEntity1);
        }
        log.info("任务准备就绪,开始验证票据");
        HashMap<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("gift_id", "300005");
        String json = HttpRequest.post(url)
                .header(Header.COOKIE, "ticket=" + ticket)
                .form(paramMap)
                .timeout(50000)
                .execute().body();
        PubgResult pubgResult = JSONUtil.toBean(json, PubgResult.class);
        if(ResultCodeEnum.ERROR_CODE.getValue().equals(pubgResult.getR())){
            log.info("票据验证失败,请检查");
        }else {
            log.info("票据验证成功");
        }
    }

}
