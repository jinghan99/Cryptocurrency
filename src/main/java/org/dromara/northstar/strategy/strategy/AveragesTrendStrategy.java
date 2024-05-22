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
import org.dromara.northstar.indicator.constant.PeriodUnit;
import org.dromara.northstar.indicator.constant.ValueType;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.trend.EMAIndicator;
import org.dromara.northstar.indicator.trend.MAIndicator;
import org.dromara.northstar.indicator.volatility.TrueRangeIndicator;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.DirectionEnum;
import org.slf4j.Logger;
import xyz.redtorch.pb.CoreEnum;

import java.util.List;

/**
 * 趋势均线策略
 * 我们采用日线的数据，并将在信号出现的下一个交易日完成买卖。之所以在下一个交易日进行交易，
 * 是为了防止出现所谓的“数据拣选”（data snooping）带来的偏差
 * <p>
 * 如果将真实波动幅度均值乘以选定的期货合约的点价，我们可以得到在正常情况下一手期货合约日内价格的变动能产生多少损益。
 * 设定尾随止损（或称移动止损，Trailing-Stop）的位置，为简单起见，止损点为距开仓以来的最好价格相当于3个真实波动幅度均值的位置。
 * 所以对于我们的
 * 多仓来说，止损点在开仓后最高价之下3个真实波动幅度均值的位置，对于空仓则是开仓后最低价之上3个真实波动幅度均值的位置。
 * * @author KevinHuangwl
 */
@StrategicComponent(AveragesTrendStrategy.NAME)
public class AveragesTrendStrategy extends AbstractStrategy    // 为了简化代码，引入一个通用的基础抽象类
        implements TradeStrategy {

    protected static final String NAME = "趋势均线策略-1.0";

    private Indicator ema10;
    private Indicator ema50;
    private Indicator ema100;
    private Indicator ema200;

    Indicator tr;

    Indicator atr;
    Indicator trDay;

    Indicator atrDay;

    private InitParams params;    // 策略的参数配置信息


    private Logger logger;

    private TradeHelper helper;

    /**
     * 记录 开仓后的 最高价格
     */
    private Double openAfterMaxPrice;


    /**
     * 记录 开仓后 最低价格
     */
    private Double openAfterMinPrice;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void onMergedBar(Bar bar) {
        logger.info("时间：{} {} 价格：{} ", bar.tradingDay(), bar.actionTime(), bar.closePrice());
        // 确保指标已经准备好再开始交易
        boolean allLineReady = tr.isReady() && ema200.isReady() && ema100.isReady() && ema50.isReady() && ema10.isReady();
        if (!allLineReady) {
            logger.debug("指标未准备就绪");
            return;
        }

        switch (ctx.getState()) {
            case EMPTY -> {
                if (isBuyOpen(bar)) {
                    helper.doBuyOpen(ctx.getDefaultVolume());
                    openAfterMaxPrice = bar.closePrice();
                    openAfterMinPrice = bar.closePrice();
                }
                if (isSellOpen(bar)) {
                    helper.doSellOpen(ctx.getDefaultVolume());
                    openAfterMaxPrice = bar.closePrice();
                    openAfterMinPrice = bar.closePrice();
                }
            }
            default -> { /* 其他情况不处理 */}
        }
        Contract c = ctx.getContract(bindedContracts().getFirst().getUnifiedSymbol());
        int longPos = ctx.getModuleAccount().getNonclosedPosition(c, CoreEnum.DirectionEnum.D_Buy);
        int shortPos = ctx.getModuleAccount().getNonclosedPosition(c, CoreEnum.DirectionEnum.D_Sell);
        List<Trade> buyTrade = ctx.getModuleAccount().getNonclosedTrades(c, CoreEnum.DirectionEnum.D_Buy);
        List<Trade> sellTrade = ctx.getModuleAccount().getNonclosedTrades(c, CoreEnum.DirectionEnum.D_Sell);
        logger.info("仓位数据 多仓 {} 空仓{}", longPos, shortPos);
//        持多仓
        if (longPos > 0) {
            doLongPos(bar, buyTrade, longPos);
            return;
        }
        // 持空仓
        if (shortPos > 0) {
            doShortPos(bar, sellTrade, shortPos);
        }
    }


    /**
     * 处理做多仓位
     * 多仓来说，止损点在开仓后最高价之下3个真实波动幅度均值的位置
     *
     * @param bar
     * @param buyTrade
     * @param longPos
     */
    private void doLongPos(Bar bar, List<Trade> buyTrade, int longPos) {
        if (bar.closePrice() > openAfterMaxPrice) {
            openAfterMaxPrice = bar.closePrice();
        }
        double stopPrice = openAfterMaxPrice - atr.value(0) * params.stopAtrMultiple;
        logger.info("做多当前价{}： 开仓后最高价 {}，止损价：{}",bar.closePrice(),openAfterMaxPrice, stopPrice);
        if (bar.closePrice() < stopPrice) {
            helper.doSellClose(longPos);
        }
    }


    /**
     * 处理做空仓位
     *
     * @param bar
     * @param sellTrade
     * @param shortPos
     */
    private void doShortPos(Bar bar, List<Trade> sellTrade, int shortPos) {
        if (bar.closePrice() < openAfterMinPrice) {
            openAfterMinPrice = bar.closePrice();
        }
        double stopPrice = openAfterMinPrice + atr.value(0) * params.stopAtrMultiple;
        logger.info("做空当前价{}： 开仓后最低价 {}，止损价：{}",bar.closePrice(),openAfterMaxPrice, stopPrice);
        if (bar.closePrice() > stopPrice) {
            helper.doBuyClose(shortPos);
        }
    }


    /**
     * 判断是否允许做多
     *
     * @return`·
     */
    private boolean isBuyOpen(Bar bar) {
        return bar.closePrice() > ema10.value(0)
                && ema10.value(0) > ema50.value(0)
                && ema10.value(0) > ema100.value(0)
                && ema10.value(0) > ema200.value(0)
                && ema50.value(0) > ema100.value(0)
                && ema100.value(0) > ema200.value(0);
    }


    /**
     * 判断是否允许开空 加入 btc 的 权重指标
     *
     * @return`·
     */
    private boolean isSellOpen(Bar bar) {
        return bar.closePrice() < ema10.value(0)
                && ema10.value(0) < ema50.value(0)
                && ema10.value(0) < ema100.value(0)
                && ema10.value(0) < ema200.value(0)
                && ema50.value(0) < ema100.value(0)
                && ema100.value(0) < ema200.value(0)
                ;
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
        this.ema10 = new EMAIndicator(makeConfig("EMA10"), 10);
        this.ema50 = new EMAIndicator(makeConfig("EMA50"), 50);
        this.ema100 = new EMAIndicator(makeConfig("EMA100"), 100);
        this.ema200 = new EMAIndicator(makeConfig("EMA200"), 200);
        this.tr = new TrueRangeIndicator(makeConfig("TR"));
        this.atr = new MAIndicator(makeConfig("ATR"), tr, params.atrLen);
        this.trDay = new TrueRangeIndicator(Configuration.builder().contract(c).indicatorName("tr_day").numOfUnits(60 * 23).build());
        this.atrDay = new MAIndicator(Configuration.builder().contract(c).indicatorName("atr_day").numOfUnits(60 * 23).build(), trDay, params.atrLen);
        logger = ctx.getLogger(getClass());
        ctx.registerIndicator(ema10);
        ctx.registerIndicator(ema50);
        ctx.registerIndicator(ema100);
        ctx.registerIndicator(ema200);
        ctx.registerIndicator(tr);
        ctx.registerIndicator(atr);
        ctx.registerIndicator(trDay);
        ctx.registerIndicator(atrDay);
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

        @Setting(label = "atr长度", type = FieldType.NUMBER, order = 1)
        private int atrLen = 20;


        @Setting(label = "风险因子", type = FieldType.NUMBER, order = 3)
        private double riskFactors = 0.002;

//        ATR stop loss multiple
        @Setting(label = "atr止损倍数", type = FieldType.NUMBER, order = 4)
        private double stopAtrMultiple = 3;


    }

    private Configuration makeConfig(String name) {
        Contract c = ctx.getContract(bindedContracts().getFirst().getUnifiedSymbol());
        return Configuration.builder().contract(c).indicatorName(name).numOfUnits(ctx.numOfMinPerMergedBar()).build();
    }
}
