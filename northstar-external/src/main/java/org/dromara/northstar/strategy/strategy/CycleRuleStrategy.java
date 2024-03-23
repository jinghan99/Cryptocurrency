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
import org.dromara.northstar.strategy.constant.StopWinEnum;
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


    private CycleRuleIndicator maxCycleRuleIndicator;
    private CycleRuleIndicator minCycleRuleIndicator;

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
        boolean allLineReady = maxCycleRuleIndicator.isReady() && minCycleRuleIndicator.isReady();
        if (!allLineReady) {
            logger.debug("指标未准备就绪");
            return;
        }
        switch (ctx.getState()) {
            case EMPTY -> {
                if (maxCycleRuleIndicator.getDirectionEnum() == DirectionEnum.UP_BREAKTHROUGH && minCycleRuleIndicator.getDirectionEnum().isUPing()) {
                    logger.info("做多");
                    helper.doBuyOpen(ctx.getDefaultVolume());
                }
                if (maxCycleRuleIndicator.getDirectionEnum() == DirectionEnum.DOWN_BREAKTHROUGH && minCycleRuleIndicator.getDirectionEnum().isDowning()) {
                    logger.info("做空");
                    helper.doSellOpen(ctx.getDefaultVolume());
                }
            }
            default -> { /* 其他情况不处理 */}
        }
        Contract c = ctx.getContract(bindedContracts().getFirst().getUnifiedSymbol());
        int longPos = ctx.getModuleAccount().getNonclosedPosition(c, CoreEnum.DirectionEnum.D_Buy);
        int shortPos = ctx.getModuleAccount().getNonclosedPosition(c, CoreEnum.DirectionEnum.D_Sell);
        List<Trade> buyTrade = ctx.getModuleAccount().getNonclosedTrades(c, CoreEnum.DirectionEnum.D_Buy);
        List<Trade> sellTrade = ctx.getModuleAccount().getNonclosedTrades(c, CoreEnum.DirectionEnum.D_Sell);
        StopWinEnum stopWinEnum = StopWinEnum.get(params.stopType);
//        持多仓
        if (longPos > 0) {
            if (maxCycleRuleIndicator.getDirectionEnum().isUPing()) {
                //止盈类型
                switch (stopWinEnum) {
                    case MIN_PERIOD -> {
                        if (minCycleRuleIndicator.getDirectionEnum().isDowning()) {
                            helper.doSellClose(longPos);
                            logger.info("小周期止盈止损 平多 {}", bar.closePrice());
                        }
                        break;
                    }
                    case TURN_DOWN_RATE -> {
                        //成本价
                        double costPrice = buyTrade.stream().mapToDouble(Trade::price).sum() / buyTrade.size();
                        double maxHigh = maxCycleRuleIndicator.getMaxHigh();
                        double range = Math.abs(maxHigh - costPrice);
                        if (bar.closePrice() > costPrice && bar.closePrice() < maxHigh) {
                            double stopWinPrice = maxHigh - params.stopWinRate * range;
                            if (bar.closePrice() < stopWinPrice) {
                                helper.doSellClose(longPos);
                                logger.info("回撤率止盈 平多 成本价 {} , close {}", costPrice, bar.closePrice());
                            }
                        }
                        if (bar.closePrice() < costPrice) {
                            double stopLossPrice = costPrice - params.stopWinRate * range;
                            if (bar.closePrice() < stopLossPrice) {
                                helper.doSellClose(longPos);
                                logger.info("回撤率止损 平多 成本价 {} , close {}", costPrice, bar.closePrice());
                            }
                        }
                    }
                }
            }
            if (maxCycleRuleIndicator.getDirectionEnum().isDowning()) {
                logger.info("止损平多做空 {}", bar.closePrice());
                helper.doSellClose(longPos);
                if (maxCycleRuleIndicator.getDirectionEnum() == DirectionEnum.DOWN_BREAKTHROUGH && minCycleRuleIndicator.getDirectionEnum().isDowning()) {
                    helper.doSellOpen(ctx.getDefaultVolume());
                }
            }
            return;
        }
        // 持空仓
        if (shortPos > 0) {
            if (maxCycleRuleIndicator.getDirectionEnum().isDowning()) {
                switch (stopWinEnum) {
                    case MIN_PERIOD -> {
                        if (minCycleRuleIndicator.getDirectionEnum().isUPing()) {
                            logger.info("小周期止盈 平空 {}", bar.closePrice());
                            helper.doBuyClose(shortPos);
                        }
                    }
                    case TURN_DOWN_RATE -> {
                        double costPrice = sellTrade.stream().mapToDouble(Trade::price).sum() / sellTrade.size();
                        double minLow = maxCycleRuleIndicator.getMinLow();
                        double range = Math.abs(costPrice - minLow);
                        if (bar.closePrice() < costPrice && bar.closePrice() > minLow) {
                            double stopWinPrice = minLow + params.stopWinRate * range;
                            if (bar.closePrice() > stopWinPrice) {
                                helper.doBuyClose(shortPos);
                                logger.info("回撤率止盈 平空 成本价 {} , close {}", costPrice, bar.closePrice());
                            }
                        }
                        if (bar.closePrice() > costPrice) {
                            double stopLossPrice = costPrice + params.stopWinRate * range;
                            if (bar.closePrice() > stopLossPrice) {
                                helper.doBuyClose(shortPos);
                                logger.info("回撤率止损 平空 成本价 {} , close {}", costPrice, bar.closePrice());
                            }
                        }
                    }
                }
            }
            if (maxCycleRuleIndicator.getDirectionEnum().isUPing()) {
                logger.info("止损平空做多 {}", bar.closePrice());
                helper.doBuyClose(shortPos);
                if (maxCycleRuleIndicator.getDirectionEnum() == DirectionEnum.UP_BREAKTHROUGH && minCycleRuleIndicator.getDirectionEnum().isUPing()) {
                    helper.doBuyOpen(ctx.getDefaultVolume());
                }
            }
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
        Contract c = ctx.getContract(bindedContracts().get(0).getUnifiedSymbol());
        // 指标的创建
        this.maxCycleRuleIndicator = new CycleRuleIndicator(Configuration.builder()
                .contract(c)
                .indicatorName("CycleMax")
                .valueType(ValueType.CLOSE)
                .numOfUnits(ctx.numOfMinPerMergedBar()).build(), params.maxPeriod);
        this.minCycleRuleIndicator = new CycleRuleIndicator(Configuration.builder()
                .contract(c)
                .indicatorName("CycleMin")
                .valueType(ValueType.CLOSE)
                .numOfUnits(ctx.numOfMinPerMergedBar()).build(), params.minPeriod);


        logger = ctx.getLogger(getClass());
        // 指标的注册
        ctx.registerIndicator(maxCycleRuleIndicator);
        ctx.registerIndicator(minCycleRuleIndicator);
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

        @Setting(label = "大周期", type = FieldType.NUMBER, order = 0)
        private int maxPeriod = 13;

        @Setting(label = "小周期", type = FieldType.NUMBER, order = 1)
        private int minPeriod = 6;

        @Setting(label = "止盈类型", type = FieldType.SELECT, options = {"小周期止盈", "回撤率止盈"}, optionsVal = {"MIN_PERIOD", "TURN_DOWN_RATE"}, order = 2)
        private String stopType = "TURN_DOWN_RATE";

        @Setting(label = "止盈率", type = FieldType.NUMBER, order = 3)
        private double stopWinRate = 0.3;

    }

}
