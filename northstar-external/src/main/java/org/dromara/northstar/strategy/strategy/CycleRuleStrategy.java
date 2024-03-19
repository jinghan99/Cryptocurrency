package org.dromara.northstar.strategy.strategy;

import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.TradeHelper;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.indicator.CycleRuleIndicator;
import org.dromara.northstar.strategy.constant.DirectionEnum;
import org.slf4j.Logger;

/**
 * 周期法则策略
 * <p>
 * * @author KevinHuangwl
 */
@StrategicComponent(CycleRuleStrategy.NAME)
public class CycleRuleStrategy extends AbstractStrategy    // 为了简化代码，引入一个通用的基础抽象类
        implements TradeStrategy {

    protected static final String NAME = "jh周期法则策略";

    private CycleRuleIndicator cycleRuleIndicator;

    private InitParams params;    // 策略的参数配置信息


    private Logger logger;

    private TradeHelper helper;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void onMergedBar(Bar bar) {
        logger.debug("{} K线数据： 开 [{}], 高 [{}], 低 [{}], 收 [{}]",
                bar.contract().unifiedSymbol(), bar.openPrice(), bar.highPrice(), bar.lowPrice(), bar.closePrice());
        // 确保指标已经准备好再开始交易
        boolean allLineReady = cycleRuleIndicator.isReady() ;
        if(!allLineReady) {
            logger.debug("指标未准备就绪");
            return;
        }
        switch (ctx.getState()) {
            case EMPTY -> {
                if(cycleRuleIndicator.getDirectionEnum() == DirectionEnum.UP) {
                    helper.doBuyOpen(1);
                }
                if(cycleRuleIndicator.getDirectionEnum() == DirectionEnum.DOWN) {
                    helper.doBuyOpen(1);
                }
            }
            case HOLDING_LONG -> {
                if(cycleRuleIndicator.getDirectionEnum() == DirectionEnum.DOWN) {
                    helper.doSellClose(1);
                }
            }
            case HOLDING_SHORT -> {
                if(cycleRuleIndicator.getDirectionEnum() == DirectionEnum.UP) {
                    helper.doBuyClose(1);
                }
            }
            default -> { /* 其他情况不处理 */}
        }
    }

    @Override
    public void onTick(Tick tick) {
        logger.info("时间：{} {} 价格：{} ", tick.actionDay(), tick.actionTime(), tick.lastPrice());

    }


    @Override
    public DynamicParams getDynamicParams() {
        return new InitParams();
    }

    @Override
    public void initWithParams(DynamicParams params) {
        this.params = (InitParams) params;
    }

    @Override
    protected void initIndicators() {
        Contract c = ctx.getContract(params.indicatorSymbol);
        // 指标的创建
        this.cycleRuleIndicator = new CycleRuleIndicator(Configuration.builder()
                .contract(c)
                .indicatorName("Cycle")
                .numOfUnits(ctx.numOfMinPerMergedBar()).build());
        logger = ctx.getLogger(getClass());
        // 指标的注册
        ctx.registerIndicator(cycleRuleIndicator);
        helper = TradeHelper.builder().context(getContext()).tradeContract(c).build();
    }

    public static class InitParams extends DynamicParams {

        @Setting(label="指标合约", order=0)
        private String indicatorSymbol;

        @Setting(label = "周期", type = FieldType.NUMBER, order = 1)
        private int period;


    }
}
