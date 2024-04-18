package org.dromara.northstar.strategy.indicator;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.constant.ValueType;
import org.dromara.northstar.indicator.helper.SimpleValueIndicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.strategy.constant.DirectionEnum;
import org.dromara.northstar.strategy.domain.FixedSizeQueue;

import java.util.Comparator;
import java.util.List;

/**
 * 成交量 周期指标 只记录 向上突破
 *
 * @author KevinHuangwl
 */
@Slf4j
public class CycleVolumeIndicator extends AbstractIndicator implements Indicator {


    @Getter
    DirectionEnum directionEnum = DirectionEnum.NON;

    @Getter
    FixedSizeQueue<DirectionEnum> fixedSizeQueue;

    private Indicator close;

    public CycleVolumeIndicator(Configuration cfg, int barCount) {
        super(cfg);
        close = new SimpleValueIndicator(cfg.toBuilder().valueType(ValueType.VOL_DELTA).cacheLength(barCount).visible(false).build());
        fixedSizeQueue = new FixedSizeQueue<>(barCount * 3);
    }


    public CycleVolumeIndicator(Configuration cfg, int barCount, int continuousCount) {
        super(cfg);
        close = new SimpleValueIndicator(cfg.toBuilder().valueType(ValueType.VOL_DELTA).cacheLength(barCount).visible(false).build());
        fixedSizeQueue = new FixedSizeQueue<>(continuousCount);
    }


    @Override
    public List<Indicator> dependencies() {
        return List.of(close);
    }

    protected Num evaluate(Num num) {
        Num num1 = getNum(num);
        fixedSizeQueue.add(directionEnum);
        return num1;
    }

    private Num getNum(Num num) {
        if (!close.isReady()) {
            return Num.NaN();
        }
        double volume = num.value();
        List<Num> prevList = close.getData().stream()
                .sorted(Comparator.comparingLong(Num::timestamp)) // 根据时间戳升序排序
                .limit(close.getData().size() - 1).toList();
        double prevMaxHigh = prevList.stream().mapToDouble(Num::value) // 提取值
                .max() // 计算最大值
                .orElse(0); // 如果流为空，则返回0
//        向上突破
        if (volume > prevMaxHigh) {
            directionEnum = DirectionEnum.UP_BREAKTHROUGH;
            return Num.of(volume, num.timestamp());
        }
        directionEnum = DirectionEnum.NON;
        return Num.of(prevMaxHigh, num.timestamp());
    }

    public double getMaxHigh() {
        return close.getData().stream().mapToDouble(Num::value).max().orElse(0);
    }

    public double getMinLow() {
        return close.getData().stream().mapToDouble(Num::value).min().orElse(0);
    }

    /**
     * 最近的 突破点
     *
     * @return
     */
    public int getContinuousDirectionCount() {
        List<DirectionEnum> allInReverseOrder = fixedSizeQueue.getAllInReverseOrder();
        if (allInReverseOrder.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = allInReverseOrder.size() - 1; i >= 0; i--) {
            if (allInReverseOrder.get(i) == DirectionEnum.UP_BREAKTHROUGH) {
                break;
            }
            count++;
        }
        return count;
    }

}
