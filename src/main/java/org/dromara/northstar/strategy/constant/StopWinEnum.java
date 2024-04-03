package org.dromara.northstar.strategy.constant;

/**
 * @author jinghan
 * @title: DirectionEnum
 * @projectName cryptocurrency
 * @description: TODO
 * @date 2024/3/19 21:00
 */
public enum StopWinEnum {
    /**
     * 小周期止盈
     */
    MIN_PERIOD,

    /**
     * 回撤率止盈
     */
    TURN_DOWN_RATE;

    /**
     * 依据name 获取 StopWinEnum
     * @param name
     * @return
     */
    public static StopWinEnum get(String name){
         return StopWinEnum.valueOf(name);
    }


}

