package com.weian.pubg.util;



import com.weian.pubg.config.QuartzDynamicJobConfig;
import com.weian.pubg.model.QuartzEntity;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;


/**
 * 定时任务工具类
 *
 * @author Weian
 * @date 2020/11/16
 **/
@Slf4j
@Component
public class QuartzUtil {


    @Qualifier("schedulerFactoryBean")
    @Autowired
    private SchedulerFactoryBean factory;

    /**
     * 指定日期执行方式
     */
    public static final String ONCE_TIME_EXECUTE_CODE = "2";

    /**
     * 启用-是否启用
     */
    public static final String STATE = "1";

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");



    /**
     * /获取JobDataMap.(Job参数对象)
     * @return JobDataMap
     */
    private JobDataMap getJobDataMap(QuartzEntity quartz) {
        JobDataMap map = new JobDataMap();
        map.put("name", quartz.getId());
        map.put("group", Scheduler.DEFAULT_GROUP);
        map.put("cronExpression", quartz.getCron());
        map.put("info", quartz);
        map.put("status", "1");
        return map;
    }




    /**
     * 获取JobDetail,JobDetail是任务的定义,而Job是任务的执行逻辑,JobDetail里会引用一个Job Class来定义
     * @param jobKey 任务key
     * @param description 说明
     * @param map 参数
     * @return JobDetail
     */
    private JobDetail geJobDetail(JobKey jobKey, String description, JobDataMap map) {
        return JobBuilder.newJob(QuartzDynamicJobConfig.class)
                .withIdentity(jobKey)
                .withDescription(description)
                .setJobData(map)
                .storeDurably()
                .build();
    }

    /**
     * 获取cron定时器
     * @return Trigger
     */
    private Trigger getCronTrigger(QuartzEntity quartz) {
        return TriggerBuilder.newTrigger()
                .withIdentity(quartz.getId(), Scheduler.DEFAULT_GROUP)
                .withSchedule(CronScheduleBuilder.cronSchedule(quartz.getCron()).withMisfireHandlingInstructionDoNothing())
                .build();
    }






    /**
     * 获取JobKey,包含Name
     * @param engineEntity 引擎
     * @return JobKey
     */
    private JobKey getJobKey(QuartzEntity engineEntity) {
        return JobKey.jobKey(engineEntity.getId(), Scheduler.DEFAULT_GROUP);
    }


    /**
     * 添加定时器
     * @param engineEntity 要添加的引擎
     * @throws SchedulerException 定时任务异常
     * @throws ParseException 时间格式异常
     */
    public void addScheduler(QuartzEntity engineEntity) throws Exception {
        try {
            Scheduler scheduler = factory.getScheduler();
            JobDataMap map = getJobDataMap(engineEntity);
            JobKey jobKey = getJobKey(engineEntity);
            JobDetail jobDetail = geJobDetail(jobKey, "测试", map);
            scheduler.scheduleJob(jobDetail, getCronTrigger(engineEntity));
        }catch (Exception e){
            log.error("添加定时器失败{}",engineEntity.getId(),e);
            //清理重复id
            throw new Exception(e);
        }


    }







}
