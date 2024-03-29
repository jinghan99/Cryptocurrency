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
import org.dromara.northstar.strategy.domain.FixedSizeQueue;

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

    @Getter
    FixedSizeQueue<DirectionEnum> fixedSizeQueue;

    private Indicator close;

    public CycleRuleIndicator(Configuration cfg, int barCount) {
        super(cfg);
        close = new SimpleValueIndicator(cfg.toBuilder().valueType(ValueType.CLOSE).cacheLength(barCount).visible(false).build());
        fixedSizeQueue = new FixedSizeQueue(barCount);
    }


    @Override
    public List<Indicator> dependencies() {
        return List.of(close);
    }

    protected Num evaluate(Num num) {
        if (!close.isReady()) {
            return Num.NaN();
        }
        try {
            double price = num.value();
            List<Num> prevList = close.getData().stream()
                    .sorted(Comparator.comparingLong(Num::timestamp)) // 根据时间戳升序排序
                    .limit(close.getData().size() - 1).toList();
            double prevMaxHigh = prevList.stream().mapToDouble(Num::value) // 提取值
                    .max() // 计算最大值
                    .orElse(0); // 如果流为空，则返回0
            double prevMinLow = prevList.stream().mapToDouble(Num::value) // 去除最后一位
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
        } finally {
            fixedSizeQueue.add(directionEnum);
        }
        return Num.NaN();
    }

    public double getMaxHigh() {
        return close.getData().stream().mapToDouble(Num::value).max().orElse(0);
    }

    public double getMinLow() {
        return close.getData().stream().mapToDouble(Num::value).min().orElse(0);
    }

    /**
     * 持续方向数
     *
     * @return
     */
    public int continuousDirectionCount() {
        List<DirectionEnum> allInReverseOrder = fixedSizeQueue.getAllInReverseOrder();
        if (allInReverseOrder.isEmpty()) {
            return 0;
        }
        int size = allInReverseOrder.size();
        DirectionEnum last = allInReverseOrder.get(size - 1);
        int count = 1;
        for (int i = allInReverseOrder.size() - 2; i >= 0; i--) {
            if (last != allInReverseOrder.get(i)) {
                break;
            }
            count++;
        }
        return count;
    }

}
