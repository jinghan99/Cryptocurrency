package org.dromara.northstar.strategy.strategy;


import lombok.Setter;
import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.TradeHelper;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.trend.EMAIndicator;
import org.dromara.northstar.indicator.trend.MAIndicator;
import org.dromara.northstar.indicator.volatility.TrueRangeIndicator;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.slf4j.Logger;
import xyz.redtorch.pb.CoreEnum;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 套利策略策略
 * 水续平仓价格:990元
 * 水续价格收益:1000-990=10元
 * 现货平全价格:990元现货价格收监:990-999=-9元价道收益:10-9=1元
 * 慢设永续价格一直为1000元资金费收益:1000*0.001*3-3元
 * 现价999元买人
 * 最终收益:1+3=4元
 * 监控 每一个币种的永续价格和现货价格 发现永续价格比现货高而且永续的资金费为正 就下单
 * 买入等额的现货和永续
 * 买入多少 做成配置
 * <p>
 * 发现永续的价格比现货价格低或者价格一直 则平仓
 * 下单用定价下单
 */
@StrategicComponent(ArbitrageStrategy.NAME)
public class ArbitrageStrategy extends AbstractStrategy implements TradeStrategy {

    protected static final String NAME = "套利策略-1.0";

    private InitParams params;	// 策略的参数配置信息

    private Logger logger;

    private TradeHelper hlp1;

    private TradeHelper hlp2;

    private long nextActionTime;

    private int holding;

    @Setter
    public static class InitParams extends DynamicParams {

        @Setting(label="合约", order = 10)
        private String unifiedSymbol1;

        @Setting(label="现货", order = 20)
        private String spot;

    }

    /***************** 以下如果看不懂，基本可以照搬 *************************/
    @Override
    public DynamicParams getDynamicParams() {
        return new InitParams();
    }

    @Override
    public void initWithParams(DynamicParams params) {
        this.params = (InitParams) params;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected void initIndicators() {
        Contract c1 = ctx.getContract(params.unifiedSymbol1);
        Contract c2 = ctx.getContract(params.spot);
        logger = ctx.getLogger(getClass());
        hlp1 = new TradeHelper(ctx, c1);
        hlp2 = new TradeHelper(ctx, c2);
    }

    @Override
    public void onTick(Tick tick) {
        if(tick.contract().unifiedSymbol().equals(params.unifiedSymbol2)) {
            return;
        }
        long now = tick.actionTimestamp();
        // 启用后，等待10秒才开始交易
        if(nextActionTime == 0) {
            nextActionTime = now + 10000;
        }
        if(now > nextActionTime) {
            nextActionTime = now + 60000;
            logger.info("开始交易");
            if(holding == 0) {
                if(ThreadLocalRandom.current().nextBoolean()) {
                    holding = 1;
                    hlp1.doBuyOpen(1);
                    hlp2.doSellOpen(1);
                } else {
                    holding = -1;
                    hlp1.doSellOpen(1);
                    hlp2.doBuyOpen(1);
                }
            } else {
                if(holding > 0) {
                    hlp1.doSellClose(1);
                    hlp2.doBuyClose(1);
                } else {
                    hlp1.doBuyClose(1);
                    hlp2.doSellClose(1);
                }
                holding = 0;
            }
        }
    }

    @Override
    public ModuleType type() {
        return ModuleType.ARBITRAGE;	// 套利策略专有标识
    }

}
