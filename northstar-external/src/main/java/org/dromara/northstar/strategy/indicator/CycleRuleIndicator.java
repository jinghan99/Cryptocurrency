package org.dromara.northstar.strategy.indicator;

import cn.hutool.core.util.NumberUtil;
import lombok.Getter;
import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.constant.ValueType;
import org.dromara.northstar.indicator.helper.SimpleValueIndicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.indicator.model.RingArray;
import org.dromara.northstar.strategy.constant.DirectionEnum;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 周期指标
 *
 * @author KevinHuangwl
 */
public class CycleRuleIndicator extends AbstractIndicator implements Indicator {


    @Getter
    DirectionEnum directionEnum = DirectionEnum.NON;

    private Indicator close;

    public CycleRuleIndicator(Configuration cfg, int barCount) {
        super(cfg);
        close = new SimpleValueIndicator(cfg.toBuilder().valueType(ValueType.CLOSE).cacheLength(barCount).visible(false).build());
    }


    @Override
    public List<Indicator> dependencies() {
        return List.of(close);
    }

    protected Num evaluate(Num num) {
        if (!close.isReady()) {
            return Num.NaN();
        }
        double price = num.value();
        double prevMaxHigh = close.getData().stream()
                .sorted(Comparator.comparingLong(Num::timestamp)) // 根据时间戳升序排序
                .limit(close.getData().size() - 1) // 去除最后一位
                .mapToDouble(Num::value) // 提取值
                .max() // 计算最大值
                .orElse(0); // 如果流为空，则返回0
        double prevMinLow = close.getData().stream()
                .sorted(Comparator.comparingLong(Num::timestamp)) // 根据时间戳升序排序
                .limit(close.getData().size() - 1) // 去除最后一位
                .mapToDouble(Num::value) // 提取值
                .min() // 计算最小值
                .orElse(0); // 如果流为空，则返回0
//        向上突破
        if (price > prevMaxHigh) {
            directionEnum = DirectionEnum.UP_BREAKTHROUGH;
            return Num.of(prevMinLow, num.timestamp());
        }
//        向下突破
        if (price < prevMinLow) {
            directionEnum = DirectionEnum.DOWN_BREAKTHROUGH;
            return Num.of(prevMaxHigh, num.timestamp());
        }
        if (directionEnum.isUPing()) {
            directionEnum = DirectionEnum.UP;
            return Num.of(prevMinLow, num.timestamp());
        }

        if (directionEnum.isDowning()) {
            directionEnum = DirectionEnum.DOWN;
            return Num.of(prevMaxHigh, num.timestamp());
        }
        return Num.NaN();
    }

    public double getMaxHigh() {
        return close.getData().stream().mapToDouble(Num::value).max().orElse(0);
    }

    public double getMinLow() {
        return close.getData().stream().mapToDouble(Num::value).min().orElse(0);
    }

}
