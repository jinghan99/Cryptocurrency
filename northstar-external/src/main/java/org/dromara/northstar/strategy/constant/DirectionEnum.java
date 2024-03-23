package org.dromara.northstar.strategy.constant;

/**
 * @author jinghan
 * @title: DirectionEnum
 * @projectName cryptocurrency
 * @description: TODO
 * @date 2024/3/19 21:00
 */
public enum DirectionEnum {

    NON,

    /**
     * 向上
     */
    UP,
    /**
     * 向上突破
     */
    UP_BREAKTHROUGH,

    /**
     * 向下
     */
    DOWN,

    /**
     * 向下突破
     */
    DOWN_BREAKTHROUGH;

    /**
     * 是否是向上
     * @return
     */
    public boolean isUPing() {
        return this == UP || this == UP_BREAKTHROUGH ;
    }

    /**
     * 是否是向下
     * @return
     */
    public boolean isDowning() {
        return this == DOWN || this == DOWN_BREAKTHROUGH;
    }

}
