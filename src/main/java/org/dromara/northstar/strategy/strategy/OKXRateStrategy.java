package org.dromara.northstar.strategy.strategy;


import lombok.Setter;
import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.TradeHelper;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.constant.ValueType;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.domain.FixedSizeQueue;
import org.dromara.northstar.strategy.indicator.OKXFundingRateIndicator;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.slf4j.Logger;
import xyz.redtorch.pb.CoreEnum;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * OKX 套利策略策略
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
@StrategicComponent(OKXRateStrategy.NAME)
public class OKXRateStrategy extends AbstractStrategy implements TradeStrategy {

    protected static final String NAME = "OKX套利策略-1.0";

    private InitParams params;    // 策略的参数配置信息

    private Logger logger;


    private Indicator okxRateIndicator;

    final FixedSizeQueue<Tick> contractPriceQueue = new FixedSizeQueue<>(10);
    final FixedSizeQueue<Tick> spotPriceQueue = new FixedSizeQueue<>(10);

    final FixedSizeQueue<Bar> contractBarQueue = new FixedSizeQueue<>(10);
    final FixedSizeQueue<Bar> spotPriceBarQueue = new FixedSizeQueue<>(10);

    private Tick contractTick;
    private Tick spotTick;

    private Bar contractBar;

    private Bar spotBar;

    @Setter
    public static class InitParams extends DynamicParams {

        @Setting(label = "合约编码", order = 10)
        private String contract;

        @Setting(label = "合约手数", type = FieldType.NUMBER, order = 11)
        private int contractNum = 1;


        @Setting(label = "现货编码", order = 21)
        private String spot;

        @Setting(label = "现货手数", type = FieldType.NUMBER, order = 21)
        private int spotNum= 1;


        @Setting(label = "合约-现货差价（%）", type = FieldType.NUMBER, order = 22)
        private double diff = 0.15;


        @Setting(label = "是否tick级别", order = 40, type = FieldType.SELECT, options = {"否", "是"}, optionsVal = {"0", "1"})
        private String isTick = "0";

    }


    @Override
    public void onMergedBar(Bar bar) {
        if (isTick()) {
            return;
        }
        if (okxRateIndicator.getData().isEmpty()) {
            logger.debug("指标未准备就绪");
            return;
        }

        if (bar.contract().unifiedSymbol().equals(params.spot)) {
            spotPriceBarQueue.add(bar);
            spotBar = bar;
        }
        if (bar.contract().unifiedSymbol().equals(params.contract)) {
            contractBarQueue.add(bar);
            contractBar = bar;
        }
        Contract c1 = ctx.getContract(params.contract);
        Contract c2 = ctx.getContract(params.spot);
//        合约 空仓
        int shortContractPos = ctx.getModuleAccount().getNonclosedPosition(c1, CoreEnum.DirectionEnum.D_Sell);
//        现货 多仓
        int longSpot = ctx.getModuleAccount().getNonclosedPosition(c2, CoreEnum.DirectionEnum.D_Buy);
        if (shortContractPos <= 0 && longSpot <= 0) {
            openBuy();
        } else {
            closeSell();
        }
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
        Contract c1 = ctx.getContract(params.contract);
        Contract c2 = ctx.getContract(params.spot);
        this.okxRateIndicator = new OKXFundingRateIndicator(
                Configuration.builder()
                        .contract(c1)
                        .indicatorName("rate_")
                        .valueType(ValueType.CLOSE)
                        .numOfUnits(ctx.numOfMinPerMergedBar()).build(),
                this.params.contract);
        ctx.registerIndicator(okxRateIndicator);
        logger = ctx.getLogger(getClass());
    }

    /**
     * @param tick
     */
    @Override
    public void onTick(Tick tick) {
        if (!isTick()) {
            return;
        }
        if (okxRateIndicator.getData().isEmpty()) {
            logger.debug("指标未准备就绪");
            return;
        }
        if (tick.contract().unifiedSymbol().equals(params.spot)) {
            spotPriceQueue.add(tick);
            spotTick = tick;
        }
        if (tick.contract().unifiedSymbol().equals(params.contract)) {
            contractPriceQueue.add(tick);
            contractTick = tick;
        }

        Contract c1 = ctx.getContract(params.contract);
        Contract c2 = ctx.getContract(params.spot);
//        合约 空仓
        int shortContractPos = ctx.getModuleAccount().getNonclosedPosition(c1, CoreEnum.DirectionEnum.D_Sell);
//        现货 多仓
        int longSpot = ctx.getModuleAccount().getNonclosedPosition(c2, CoreEnum.DirectionEnum.D_Buy);
        if (shortContractPos <= 0 && longSpot <= 0) {
            openBuy();
        } else {
            closeSell();
        }
    }


    /**
     * 执行 开仓逻辑
     * 发现永续价格比现货高而且永续的资金费为正 就下单
     * 买入等额的现货和永续
     * <p>
     * 合约 - 现货 > 0 开仓
     * 合约 - 现货 < 0 平仓
     */
    private void openBuy() {
        // 校验时间 是否同一分钟
        if (!verifyDate() || verifyNoPass()) {
            return;
        }
        if (okxRateIndicator.value(0) <= 0) {
            return;
        }
        Contract swap = ctx.getContract(params.contract);
        Contract spot = ctx.getContract(params.spot);
//        合约 空仓
        int shortContractPos = ctx.getModuleAccount().getNonclosedPosition(swap, CoreEnum.DirectionEnum.D_Sell);
//        现货 多仓
        int longSpot = ctx.getModuleAccount().getNonclosedPosition(spot, CoreEnum.DirectionEnum.D_Buy);
//        合约价格
        double contractPrice = isTick() ? contractTick.lastPrice() : contractBar.closePrice();
//        现货价格
        double spotPrice = isTick() ? spotTick.lastPrice() : spotBar.closePrice();
//        计算差价百分比
        double calculatedDiffPrice = calculatePercentageDifference(spotPrice, contractPrice);
//       无持仓  合约 - 现货 > 设置差值 并且 资金费为正
        if (shortContractPos <= 0 && longSpot <= 0 && calculatedDiffPrice > this.params.diff && okxRateIndicator.value(0) > 0) {
            ctx.submitOrderReq(TradeIntent.builder()
                    .contract(swap)
                    .operation(SignalOperation.SELL_OPEN)
                    .priceType(PriceType.LIMIT_PRICE)
                    .price(contractPrice)
                    .volume(params.contractNum)
                    .timeout(5000)
                    .build());
            logger.info("空开合约");
            ctx.submitOrderReq(TradeIntent.builder()
                    .contract(spot)
                    .operation(SignalOperation.BUY_OPEN)
                    .priceType(PriceType.LIMIT_PRICE)
                    .price(spotPrice)
                    .volume(params.spotNum)
                    .timeout(5000)
                    .build());
            logger.info("多开现货");
        }
    }

    /**
     * 检验数据
     *
     * @return
     */
    private boolean verifyNoPass() {
        if (isTick() && (this.spotTick == null || this.contractTick == null)) {
            return true;
        }
        return !isTick() && (this.spotBar == null || this.contractBar == null);
    }


    /**
     * 执行 平仓逻辑
     * 发现永续的价格比现货价格低或者价格相等 则平仓
     */
    private void closeSell() {
        //        校验时间
        if (!verifyDate() || verifyNoPass()) {
            return;
        }
        Contract swap = ctx.getContract(params.contract);
        Contract spot = ctx.getContract(params.spot);
//        合约 空仓
        int shortContractPos = ctx.getModuleAccount().getNonclosedPosition(swap, CoreEnum.DirectionEnum.D_Sell);
//        现货 多仓
        int longSpots = ctx.getModuleAccount().getNonclosedPosition(spot, CoreEnum.DirectionEnum.D_Buy);
//        合约价格
        double contractPrice = isTick() ? contractTick.lastPrice() : contractBar.closePrice();
//        现货价格
        double spotPrice = isTick() ? spotTick.lastPrice() : spotBar.closePrice();
        //        计算差价百分比
        double calculatedDiffPrice = calculatePercentageDifference(spotPrice, contractPrice);

//        发现 差价小于等于 0
        if (calculatedDiffPrice < -1 * this.params.diff) {
            if (shortContractPos > 0) {
                ctx.submitOrderReq(TradeIntent.builder()
                        .contract(swap)
                        .operation(SignalOperation.BUY_CLOSE)
                        .priceType(PriceType.LIMIT_PRICE)
                        .price(contractPrice)
                        .volume(shortContractPos)
                        .timeout(5000)
                        .build());
                logger.info("买平合约 {} 仓位 {}", contractTick.lastPrice(), shortContractPos);
            }
            if (longSpots > 0) {
                ctx.submitOrderReq(TradeIntent.builder()
                        .contract(spot)
                        .operation(SignalOperation.SELL_CLOSE)
                        .priceType(PriceType.LIMIT_PRICE)
                        .price(spotPrice)
                        .volume(longSpots)
                        .timeout(5000)
                        .build());
                logger.info("卖平现货 {} 仓位 {}", spotTick.lastPrice(), longSpots);
            }
        }
    }


    /**
     * 校验时间 两个时间是否小于 2 秒
     *
     * @return
     */
    public boolean verifyDate() {
        if (isTick()) {
            if (contractTick == null || spotTick == null) {
                return false;
            }
            Duration duration = Duration.between(contractTick.actionTime(), spotTick.actionTime());
            return duration.toMillis() < 2000;
        }
        if (contractBar == null || spotBar == null) {
            return false;
        }
        Duration duration = Duration.between(contractBar.actionTime(), spotBar.actionTime());
        return duration.toMillis() < 10000;
    }

    @Override
    public ModuleType type() {
        return ModuleType.ARBITRAGE;    // 套利策略 专有标识
    }


    /**
     * 是否 tick 级别 监听
     *
     * @return
     */
    public boolean isTick() {
        return params.isTick.equals("1");
    }


    /**
     * 计算小数位
     *
     * @param number
     * @return
     */
    private int calculateDecimalPlaces(double number) {
        BigDecimal bd = new BigDecimal(Double.toString(number));
        String[] parts = bd.toPlainString().split("\\.");
        if (parts.length > 1) {
            return parts[1].length();
        } else {
            return 0; // 如果没有小数部分，返回0
        }
    }

    /**
     * 计算差价百分比的方法
     * 合约 - 现货 > 0 开仓
     * 合约 - 现货 < 0 平仓
     *
     * @param spotPrice
     * @param contractPrice
     * @return
     */
    public static double calculatePercentageDifference(double spotPrice, double contractPrice) {
        double difference = Math.abs(spotPrice - contractPrice);
        return (difference / spotPrice) * 100;
    }
}
