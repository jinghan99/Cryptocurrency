package org.dromara.northstar.strategy.strategy;

import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.TradeHelper;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.constant.ValueType;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.trend.MAIndicator;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.DirectionEnum;
import org.dromara.northstar.strategy.indicator.CycleRuleIndicator;
import org.slf4j.Logger;
import xyz.redtorch.pb.CoreEnum;

import java.util.List;

/**
 * 周期法则策略
 * <p>
 * * @author KevinHuangwl
 */
@StrategicComponent(CycleBarStrategy.NAME)
public class CycleBarStrategy extends AbstractStrategy    // 为了简化代码，引入一个通用的基础抽象类
        implements TradeStrategy {

    protected static final String NAME = "jhCycleBar止盈法则策略";


    private CycleRuleIndicator maxCycleRuleIndicator;
    private CycleRuleIndicator minCycleRuleIndicator;

    private Indicator maIndicator;

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
        boolean allLineReady = maxCycleRuleIndicator.isReady() && minCycleRuleIndicator.isReady() && maIndicator.isReady();
        if (!allLineReady) {
            logger.debug("指标未准备就绪");
            return;
        }
        logger.info("大周期方向: {}，连续数{} ", maxCycleRuleIndicator.getDirectionEnum(), maxCycleRuleIndicator.continuousDirectionCount());
        logger.info("小周期方向: {}，连续数{} , 数据 {}", minCycleRuleIndicator.getDirectionEnum(), minCycleRuleIndicator.continuousDirectionCount(), minCycleRuleIndicator.getDataByAsc());
        logger.info("{} K线数据：  收 [{}]  ma： [{}] ", bar.contract().unifiedSymbol(), bar.closePrice(), maIndicator.value(0));
        switch (ctx.getState()) {
            case EMPTY -> {
                if (isBuyOpen(bar)) {
                    logger.info("做多 {} K线数据：  收 [{}] 指标方向: maxCycle [{}] ,连续数{}、  minCycle [{}] 连续数{} 、 ma： [{}] ",
                            bar.contract().unifiedSymbol(), bar.closePrice(), maxCycleRuleIndicator.getDirectionEnum(), maxCycleRuleIndicator.continuousDirectionCount(), minCycleRuleIndicator.getDirectionEnum(), minCycleRuleIndicator.continuousDirectionCount(), maIndicator.value(0));
                    helper.doBuyOpen(ctx.getDefaultVolume());
                }
                if (isSellOpen(bar)) {
                    logger.info("做空 {} K线数据：  收 [{}] 指标方向: maxCycle [{}] ,连续数{}、  minCycle [{}] 连续数{} 、 ma： [{}] ",
                            bar.contract().unifiedSymbol(), bar.closePrice(), maxCycleRuleIndicator.getDirectionEnum(), maxCycleRuleIndicator.continuousDirectionCount(), minCycleRuleIndicator.getDirectionEnum(), minCycleRuleIndicator.continuousDirectionCount(), maIndicator.value(0));
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
//        持多仓
        if (longPos > 0) {
            if (maxCycleRuleIndicator.getDirectionEnum().isUPing()) {
                if (minCycleRuleIndicator.getDirectionEnum().isDowning()) {
                    double costPrice = buyTrade.stream().mapToDouble(Trade::price).sum() / buyTrade.size();
                    if (bar.closePrice() > costPrice + params.smallPeriodTakeProfitMinPoints) {
                        helper.doSellClose(longPos);
                        logger.info("小周期bar止盈 平多 现价{}，成本价{} ,小周期数据 {}", bar.closePrice(), costPrice, minCycleRuleIndicator.getDataByAsc());
                    }
                }
            }
            if (maxCycleRuleIndicator.getDirectionEnum().isDowning()) {
                logger.info("止损平多做空 {}", bar.closePrice());
                helper.doSellClose(longPos);
                if (isSellOpen(bar)) {
                    helper.doSellOpen(ctx.getDefaultVolume());
                }
            }
            return;
        }
        // 持空仓
        if (shortPos > 0) {
            if (maxCycleRuleIndicator.getDirectionEnum().isDowning()) {
                if (minCycleRuleIndicator.getDirectionEnum().isUPing()) {
                    double costPrice = sellTrade.stream().mapToDouble(Trade::price).sum() / sellTrade.size();
                    if (bar.closePrice() < costPrice - params.smallPeriodTakeProfitMinPoints) {
                        logger.info("小周期tick止盈 平空 现价{}，成本价{} ,小周期数据 {}", bar.closePrice(), costPrice, minCycleRuleIndicator.getDataByAsc());
                        helper.doBuyClose(shortPos);
                    }
                }
            }
            if (maxCycleRuleIndicator.getDirectionEnum().isUPing()) {
                logger.info("止损平空做多 {}", bar.closePrice());
                helper.doBuyClose(shortPos);
                if (isBuyOpen(bar)) {
                    helper.doBuyOpen(ctx.getDefaultVolume());
                }
            }
        }
    }


    @Override
    public void onTick(Tick tick) {
        logger.debug("时间：{} {} 价格：{} ", tick.actionDay(), tick.actionTime(), tick.lastPrice());
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
        Contract c = ctx.getContract(bindedContracts().getFirst().getUnifiedSymbol());
        // 指标的创建
        this.maxCycleRuleIndicator = new CycleRuleIndicator(Configuration.builder()
                .contract(c)
                .indicatorName("Cycle_max_" + params.maxPeriod)
                .valueType(ValueType.CLOSE)
                .numOfUnits(ctx.numOfMinPerMergedBar()).build(), params.maxPeriod);

        this.minCycleRuleIndicator = new CycleRuleIndicator(Configuration.builder()
                .contract(c)
                .indicatorName("Cycle_min_" + params.minPeriod)
                .valueType(ValueType.CLOSE)
                .numOfUnits(params.minMinute).build(), params.minPeriod);

        this.maIndicator = new MAIndicator(Configuration.builder()
                .contract(c)
                .indicatorName("ma_" + params.maLen)
                .valueType(ValueType.CLOSE)
                .numOfUnits(ctx.numOfMinPerMergedBar()).build(), params.maLen);

        logger = ctx.getLogger(getClass());
        // 指标的注册
        ctx.registerIndicator(maxCycleRuleIndicator);
        ctx.registerIndicator(minCycleRuleIndicator);
        ctx.registerIndicator(maIndicator);
        // 指标的注册
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

    /**
     * 判断是否允许做多
     *
     * @return`·
     */
    private boolean isBuyOpen(Bar bar) {
        return maxCycleRuleIndicator.getDirectionEnum() == DirectionEnum.UP_BREAKTHROUGH && minCycleRuleIndicator.getDirectionEnum().isUPing() && bar.closePrice() > maIndicator.value(0) && minCycleRuleIndicator.continuousDirectionCount() >= params.smallPeriodOpenDuration;
    }


    /**
     * 判断是否允许开空
     *
     * @return`·
     */
    private boolean isSellOpen(Bar bar) {
        return maxCycleRuleIndicator.getDirectionEnum() == DirectionEnum.DOWN_BREAKTHROUGH && minCycleRuleIndicator.getDirectionEnum().isDowning() && bar.closePrice() < maIndicator.value(0) && minCycleRuleIndicator.continuousDirectionCount() >= params.smallPeriodOpenDuration;
    }


    public static class InitParams extends DynamicParams {

        @Setting(label = "大周期", type = FieldType.NUMBER, order = 0)
        private int maxPeriod = 75;

        @Setting(label = "大周期MA均线", type = FieldType.NUMBER, order = 1)
        private int maLen = 90;

        @Setting(label = "小周期bar级别", type = FieldType.NUMBER, order = 2)
        private int minMinute = 1;

        @Setting(label = "小周期开仓持续数", type = FieldType.NUMBER, order = 3)
        private int smallPeriodOpenDuration = 3;

        @Setting(label = "小周期止盈周期", type = FieldType.NUMBER, order = 4)
        private int minPeriod = 6;

        @Setting(label = "小周期止盈最小点数", type = FieldType.NUMBER, order =5)
        private double smallPeriodTakeProfitMinPoints = 0.0002;


    }

}
