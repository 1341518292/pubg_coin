package com.weian.pubg.enums;

/**
 * @author Weian
 * @date 2023/10/19 10:17
 */
public enum ResultCodeEnum {

    //积分不足
    NO_INTEGRAL(12),
    SUCCESS(0),
    //票据失效
    ERROR_CODE(2);

    private final Integer value;

    public Integer getValue(){
        return this.value;
    }

    ResultCodeEnum(Integer value) {
        this.value = value;
    }

}
