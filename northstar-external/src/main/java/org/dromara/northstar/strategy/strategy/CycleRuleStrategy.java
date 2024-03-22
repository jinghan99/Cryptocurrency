package org.dromara.northstar.strategy.strategy;

import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.TradeHelper;
import org.dromara.northstar.indicator.constant.ValueType;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.indicator.CycleRuleIndicator;
import org.dromara.northstar.strategy.constant.DirectionEnum;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.slf4j.Logger;
import xyz.redtorch.pb.CoreEnum;

import java.util.List;

/**
 * 周期法则策略
 * <p>
 * * @author KevinHuangwl
 */
@StrategicComponent(CycleRuleStrategy.NAME)
public class CycleRuleStrategy extends AbstractStrategy    // 为了简化代码，引入一个通用的基础抽象类
        implements TradeStrategy {

    protected static final String NAME = "jh周期法则策略";

    /**
     * 成本价
     */
    private double costPrice;

    private CycleRuleIndicator cycleRuleIndicator;

    private InitParams params;    // 策略的参数配置信息


    private Logger logger;

    private TradeHelper helper;

    /**
     * 记录上一个 K 线方向
     */
    private DirectionEnum preDirection;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void onMergedBar(Bar bar) {
        logger.debug("{} K线数据： 开 [{}], 高 [{}], 低 [{}], 收 [{}]",
                bar.contract().unifiedSymbol(), bar.openPrice(), bar.highPrice(), bar.lowPrice(), bar.closePrice());
        // 确保指标已经准备好再开始交易
        boolean allLineReady = cycleRuleIndicator.isReady();
        if (!allLineReady) {
            logger.debug("指标未准备就绪");
            return;
        }
        Contract c = ctx.getContract(bindedContracts().get(0).getUnifiedSymbol());
        int longPos = ctx.getModuleAccount().getNonclosedPosition(c, CoreEnum.DirectionEnum.D_Buy);
        int shortPos = ctx.getModuleAccount().getNonclosedPosition(c, CoreEnum.DirectionEnum.D_Sell);
        List<Trade> buyTrade = ctx.getModuleAccount().getNonclosedTrades(c, CoreEnum.DirectionEnum.D_Buy);
        List<Trade> sellTrade = ctx.getModuleAccount().getNonclosedTrades(c, CoreEnum.DirectionEnum.D_Sell);
//        持多仓
        if (longPos > 0) {
//            成本价
            double costPrice = buyTrade.stream().mapToDouble(trade -> trade.price()).sum() / buyTrade.size();
            if (costPrice > 0 && cycleRuleIndicator.getDirectionEnum() == DirectionEnum.UP) {
                double maxHigh = cycleRuleIndicator.getMaxHigh();
                double range = maxHigh - costPrice;
//                止盈
                if (range > 0 && bar.closePrice() < maxHigh) {
                    double stopWinPrice = maxHigh - params.stopWinRate * range;
                    double start = costPrice + params.stopWinRate * range;
                    if (bar.closePrice() > start && bar.closePrice() < stopWinPrice) {
                        helper.doSellClose(longPos);
                        logger.info("平多");
                    }
                }
            }
            if (cycleRuleIndicator.getDirectionEnum() == DirectionEnum.DOWN) {
                logger.info("平多做空");
                helper.doSellClose(longPos);
                helper.doSellOpen(ctx.getDefaultVolume());
            }
            return;
        }
        // 持空仓
        if (shortPos > 0) {
            double costPrice = sellTrade.stream().mapToDouble(trade -> trade.price()).sum() / sellTrade.size();
            if (costPrice > 0 && cycleRuleIndicator.getDirectionEnum() == DirectionEnum.DOWN) {
                double minLow = cycleRuleIndicator.getMinLow();
                double range = costPrice - minLow;
                if (range > 0 && bar.closePrice() > minLow) {
                    double stopWinPrice = minLow + params.stopWinRate * range;
                    double start = costPrice - params.stopWinRate * range;
                    if (bar.closePrice() < start && bar.closePrice() > stopWinPrice) {
                        logger.info("平空");
                        helper.doBuyClose(ctx.getDefaultVolume());
                    }
                }
            }
            if (cycleRuleIndicator.getDirectionEnum() == DirectionEnum.UP) {
                logger.info("平空做多");
                helper.doBuyClose(shortPos);
                helper.doBuyOpen(ctx.getDefaultVolume());
            }
            return;
        }

        switch (ctx.getState()) {
            case EMPTY -> {
//                if (preDirection != null && cycleRuleIndicator.getDirectionEnum() == preDirection) {
//                    break;
//                }
                if (cycleRuleIndicator.getDirectionEnum() == DirectionEnum.UP) {
                    logger.info("做多");
                    helper.doBuyOpen(ctx.getDefaultVolume());
                }
                if (cycleRuleIndicator.getDirectionEnum() == DirectionEnum.DOWN) {
                    logger.info("做空");
                    helper.doSellOpen(ctx.getDefaultVolume());
                }
            }
            default -> { /* 其他情况不处理 */}
        }
        preDirection = cycleRuleIndicator.getDirectionEnum();
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
        Contract c = ctx.getContract(bindedContracts().get(0).getUnifiedSymbol());
        // 指标的创建
        this.cycleRuleIndicator = new CycleRuleIndicator(Configuration.builder()
                .contract(c)
                .indicatorName("Cycle")
                .valueType(ValueType.CLOSE)
                .numOfUnits(ctx.numOfMinPerMergedBar()).build(), params.period);
        logger = ctx.getLogger(getClass());
        // 指标的注册
        ctx.registerIndicator(cycleRuleIndicator);
        helper = TradeHelper.builder().context(getContext()).tradeContract(c).build();
    }

    @Override
    public void onTrade(Trade trade) {
        // 如果策略不关心成交反馈，可以不重写
        logger.info("模组成交 [{} {} {} 操作：{}{} {}手 {}]", trade.contract().unifiedSymbol(),
                trade.tradeDate(), trade.tradeTime(), FieldUtils.chn(trade.direction()), FieldUtils.chn(trade.offsetFlag()),
                trade.volume(), trade.price());
        logger.info("当前模组净持仓：[{}]", ctx.getModuleAccount().getNonclosedNetPosition(trade.contract()));
        logger.info("当前模组状态：{}", ctx.getState());
    }

    public static class InitParams extends DynamicParams {

        @Setting(label = "周期", type = FieldType.NUMBER, order = 0)
        private int period;

        @Setting(label = "止盈", type = FieldType.NUMBER, order = 0)
        private double stopWinRate;


    }

}
