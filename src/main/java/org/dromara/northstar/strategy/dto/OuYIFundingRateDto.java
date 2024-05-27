package org.dromara.northstar.strategy.dto;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * OuYIFundingRateDto 表示资金费率信息的数据结构。
 */
@Data
@NoArgsConstructor
public class OuYIFundingRateDto {

    /**
     * 响应代码。
     */
    private String code;

    /**
     * 资金费率数据列表。
     */
    private List<DataDTO> data;

    /**
     * 响应消息。
     */
    private String msg;

    /**
     * DataDTO 表示特定产品的详细资金费率信息。
     */
    @NoArgsConstructor
    @Data
    public static class DataDTO {

        /**
         * 资金费率。
         */
        private BigDecimal fundingRate;

        /**
         * 资金费时间，Unix 时间戳的毫秒数格式。
         */
        private String fundingTime;

        /**
         * 产品 ID，例如 BTC-USD-SWAP。
         */
        private String instId;

        /**
         * 产品类型，SWAP 表示永续合约。
         * 可能的值：SWAP
         */
        private String instType;

        /**
         * 下一期的预测资金费率上限。
         */
        private BigDecimal maxFundingRate;

        /**
         * 资金费收取逻辑。
         * 可能的值：current_period（当期收），next_period（跨期收）。
         */
        private String method;

        /**
         * 下一期的预测资金费率下限。
         */
        private BigDecimal minFundingRate;

        /**
         * 下一期预测资金费率。
         * 当收取逻辑为 current_period 时，nextFundingRate 字段将返回空字符串。
         */
        private BigDecimal nextFundingRate;

        /**
         * 下一期资金费时间，Unix 时间戳的毫秒数格式。
         */
        private Long nextFundingTime;

        /**
         * 溢价，为合约的中间价和指数价格的差异。
         */
        private BigDecimal premium;

        /**
         * 结算资金费率。
         * 如果 settState 为 processing，则该字段代表用于本轮结算的资金费率；
         * 如果 settState 为 settled，则该字段代表用于上轮结算的资金费率。
         */
        private BigDecimal settFundingRate;

        /**
         * 资金费率结算状态。
         * 可能的值：processing（结算中），settled（已结算）。
         */
        private String settState;

        /**
         * 数据更新时间，Unix 时间戳的毫秒数格式。
         */
        private Long ts;
    }
}

