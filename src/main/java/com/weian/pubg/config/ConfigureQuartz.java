package com.weian.pubg.config;

import com.weian.pubg.util.SpringUtils;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * Quartz的核心配置类
 * @author Weian
 * @date 2022/4/22
 **/
@Configuration
public class ConfigureQuartz implements ApplicationContextAware {

    @Bean
    public ThreadPoolExecutor poolExecutor(){
        return new ThreadPoolExecutor(20, 20,
                30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(30), new ThreadPoolExecutor.DiscardOldestPolicy());

    }


    ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        SpringUtils.setApplicationContext(applicationContext);
    }

    /**
     * 配置JobFactory
     * @param applicationContext 上下文
     * @return JobFactory
     */
    @Bean
    public JobFactory jobFactory(ApplicationContext applicationContext) {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    /**
     * Spring 可管理的 Quartz Scheduler Factory
     * @param config 重写QuartzJobInstance
     * @return SchedulerFactoryBean
     */
    @Bean("schedulerFactoryBean")
    @Scope("singleton")
    SchedulerFactoryBean schedulerFactoryBean(@Autowired QuartzJobInstanceConfig config){
        // SchedulerFactoryBean 提供自定义的定时调度配置
        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        // 替换掉默认的 Job 工厂
        schedulerFactory.setJobFactory(config);
        schedulerFactory.setApplicationContext(config.applicationContext);
        return schedulerFactory;
    }

    /**
     * 配置JobFactory,为quartz作业添加自动连接支持
     */
    public static final class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory implements
            ApplicationContextAware {
        private transient AutowireCapableBeanFactory beanFactory;
        @Override
        public void setApplicationContext(final ApplicationContext context) {
            beanFactory = context.getAutowireCapableBeanFactory();
        }
        @Override
        protected Object createJobInstance(final TriggerFiredBundle bundle) throws Exception {
            final Object job = super.createJobInstance(bundle);
            beanFactory.autowireBean(job);
            return job;
        }
    }

}
