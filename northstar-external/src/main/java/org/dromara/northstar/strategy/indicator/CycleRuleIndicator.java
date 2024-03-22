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
import java.util.List;

/**
 * 周期指标
 *
 * @author KevinHuangwl
 */
public class CycleRuleIndicator extends AbstractIndicator implements Indicator {


    @Getter
    DirectionEnum directionEnum;

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
        double maxHigh = close.getData().stream().mapToDouble(Num::value).max().orElse(0);
        double minLow = close.getData().stream().mapToDouble(Num::value).min().orElse(0);
        if (price >= maxHigh && (directionEnum == null || directionEnum == DirectionEnum.DOWN)) {
            directionEnum = DirectionEnum.UP;
            return Num.of(minLow, num.timestamp());
        }
        if (price <= minLow && (directionEnum == null || directionEnum == DirectionEnum.UP)) {
            directionEnum = DirectionEnum.DOWN;
            return Num.of(maxHigh, num.timestamp());
        }
        if (directionEnum == DirectionEnum.UP) {
            return Num.of(minLow, num.timestamp());
        }
        return Num.of(maxHigh, num.timestamp());
    }

    public double getMaxHigh() {
        return close.getData().stream().mapToDouble(Num::value).max().orElse(0);
    }

    public double getMinLow() {
        return close.getData().stream().mapToDouble(Num::value).min().orElse(0);
    }

}
