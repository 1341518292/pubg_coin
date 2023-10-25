package com.weian.pubg.model;

import lombok.Data;

import java.util.List;

/**
 * @author Weian
 * @date 2023/10/17 9:43
 */
@Data
public class QuartzEntity {
    private String id;
    private String cron;
    private List<QuartzEntity> quartzEntityList;
    private String giftId;

    public QuartzEntity(String id, String cron, String giftId){
        this.id = id;
        this.cron = cron;
        this.giftId = giftId;
    }

}
