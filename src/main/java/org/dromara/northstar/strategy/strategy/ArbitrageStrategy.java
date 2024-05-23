package org.dromara.northstar.strategy.strategy;


import lombok.Setter;
import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.TradeHelper;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.constant.PeriodUnit;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.domain.FixedSizeQueue;
import org.dromara.northstar.strategy.indicator.OuYiFundingRateIndicator;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.slf4j.Logger;
import xyz.redtorch.pb.CoreEnum;

import java.time.Duration;

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

    private InitParams params;    // 策略的参数配置信息

    private Logger logger;

    private TradeHelper contractHelper;

    private TradeHelper spotTradeHelper;

    private Indicator fundingRateIndicator;

    final FixedSizeQueue<Tick> contractPriceQueue = new FixedSizeQueue<>(10);
    final FixedSizeQueue<Tick> spotPriceQueue = new FixedSizeQueue<>(10);

    private Tick contractTick;
    private Tick spotTick;

    @Setter
    public static class InitParams extends DynamicParams {

        @Setting(label = "合约", order = 10)
        private String contract;

        @Setting(label = "合约费率id", order = 11)
        private String contractInstId = "BTC-USD-SWAP";

        @Setting(label = "合约费率更新周期(分钟)", type = FieldType.NUMBER, order = 12)
        private int updateInstIdMin = 20;

        @Setting(label = "现货", order = 21)
        private String spot;

        @Setting(label = "永续价格比现货高 差异值", type = FieldType.NUMBER, order = 22)
        private double diff = 0.3;


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
        this.fundingRateIndicator = new OuYiFundingRateIndicator(
                Configuration.builder()
                        .contract(c1)
                        .indicatorName("rate")
                        .period(PeriodUnit.MINUTE)
                        .numOfUnits(this.params.updateInstIdMin)
                        .build(),
                this.params.contractInstId, 20);
        ctx.registerIndicator(fundingRateIndicator);
        logger = ctx.getLogger(getClass());
        contractHelper = new TradeHelper(ctx, c1);
        spotTradeHelper = new TradeHelper(ctx, c2);
    }

    /**
     *
     *
     * @param tick
     */
    @Override
    public void onTick(Tick tick) {
        if (tick.contract().unifiedSymbol().equals(params.spot)) {
            spotPriceQueue.add(tick);
            spotTick = tick;
        }
        if (tick.contract().unifiedSymbol().equals(params.contract)) {
            contractPriceQueue.add(tick);
            contractTick = tick;
        }
        openBuy();
        closeSell();
    }


    /**
     * 执行 开仓逻辑
     * 发现永续价格比现货高而且永续的资金费为正 就下单
     * 买入等额的现货和永续
     */
    private void openBuy() {
        //        校验时间
        if (!verifyDate()) {
            return;
        }
//        资金费为小于0
        if (fundingRateIndicator.value(0) <= 0) {
            return;
        }

        Contract c1 = ctx.getContract(params.contract);
        Contract c2 = ctx.getContract(params.spot);
//        合约 空仓
        int shortContractPos = ctx.getModuleAccount().getNonclosedPosition(c1, CoreEnum.DirectionEnum.D_Sell);
//        现货 多仓
        int longSpot = ctx.getModuleAccount().getNonclosedPosition(c2, CoreEnum.DirectionEnum.D_Buy);

        double diffPrice = contractTick.lastPrice() - spotTick.lastPrice();

//       无持仓
        if (shortContractPos <= 0 && longSpot <= 0 && diffPrice == this.params.diff) {
            ctx.submitOrderReq(TradeIntent.builder()
                    .contract(c1)
                    .operation(SignalOperation.SELL_OPEN)
                    .priceType(PriceType.LIMIT_PRICE)
                    .price(contractTick.lastPrice())
                    .volume(ctx.getDefaultVolume())
                    .timeout(5000)
                    .build());
            logger.info("空开合约");
            ctx.submitOrderReq(TradeIntent.builder()
                    .contract(c1)
                    .operation(SignalOperation.BUY_OPEN)
                    .priceType(PriceType.LIMIT_PRICE)
                    .price(spotTick.lastPrice())
                    .volume(ctx.getDefaultVolume())
                    .timeout(5000)
                    .build());
            logger.info("多开现货");
        }
    }


    /**
     * 执行 平仓逻辑
     * 发现永续的价格比现货价格低或者价格相等 则平仓
     */
    private void closeSell() {
        //        校验时间
        if (!verifyDate()) {
            return;
        }
//        资金费为小于0
        if (fundingRateIndicator.value(0) <= 0) {
            return;
        }

        Contract c1 = ctx.getContract(params.contract);
        Contract c2 = ctx.getContract(params.spot);
//        合约 空仓
        int shortContractPos = ctx.getModuleAccount().getNonclosedPosition(c1, CoreEnum.DirectionEnum.D_Sell);
//        现货 多仓
        int longSpot = ctx.getModuleAccount().getNonclosedPosition(c2, CoreEnum.DirectionEnum.D_Buy);
        double diffPrice = contractTick.lastPrice() - spotTick.lastPrice();

//       无持仓
        if (shortContractPos > 0 && longSpot > 0 && diffPrice <= 0 ) {
            contractHelper.doBuyClose(contractTick.lastPrice(), shortContractPos, 10000, p -> true);
            logger.info("买平合约 {} 仓位 {}",contractTick.lastPrice(), shortContractPos);
            spotTradeHelper.doSellClose(spotTick.lastPrice(), longSpot, 10000, p -> true);
            logger.info("卖平现货 {} 仓位 {}",spotTick.lastPrice(), longSpot);
        }
    }


    /**
     * 校验时间 两个时间是否小于 2 秒
     *
     * @return
     */
    public boolean verifyDate() {
        if(contractTick ==null || spotTick ==null){
            return false;
        }
        Duration duration = Duration.between(contractTick.actionTime(), spotTick.actionTime());
        return duration.toMillis() < 2000;
    }

    @Override
    public ModuleType type() {
        return ModuleType.ARBITRAGE;    // 套利策略专有标识
    }

}
