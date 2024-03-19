package org.dromara.northstar.strategy.indicator;

import cn.hutool.core.util.NumberUtil;
import lombok.Getter;
import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.strategy.constant.DirectionEnum;

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

    public CycleRuleIndicator(Configuration cfg) {
        super(cfg);
    }


    @Override
    public List<Indicator> dependencies() {
        return Collections.emptyList();
    }

    protected Num evaluate(Num num) {
        if (!this.isReady()) {
            return num;
        }
        double price = NumberUtil.round(num.value(), 4).doubleValue();
        double maxHigh = this.getData().stream().mapToDouble(Num::value).max().orElse(0);
        double minLow = this.getData().stream().mapToDouble(Num::value).min().orElse(0);
        if (price > maxHigh && (directionEnum == null || directionEnum == DirectionEnum.DOWN)) {
            directionEnum = DirectionEnum.UP;
        }
        if (price < minLow && (directionEnum == null || directionEnum == DirectionEnum.UP)) {
            directionEnum = DirectionEnum.DOWN;
        }
        return num;
    }

}
