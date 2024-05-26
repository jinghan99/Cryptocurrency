package org.dromara.northstar.strategy.indicator;

import cn.hutool.core.convert.Convert;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSON;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.strategy.dto.OuYIFundingRateDto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 欧意 资金费率
 * https://www.okx.com/api/v5/public/funding-rate?instId=BTC-USDT-SWAP
 */
@Slf4j
public class OKXFundingRateIndicator extends AbstractIndicator implements Indicator {


    private static final String RATE_URL = "https://www.okx.com/api/v5/public/funding-rate?instId=";


    /**
     * 提前更新时间 1小时
     */
    private static final Long advanceTime = 60 * 60 * 1000L;

    //    5分钟内 不重复更新
    private static final int syncTime = 5 * 60 * 1000;


    /**
     * 产品ID ，如 BTC-USD-SWAP 仅适用于永续
     */
    private String instId;

    private BigDecimal lastFundingRate = BigDecimal.ZERO;

    @Getter
    private OuYIFundingRateDto.DataDTO lastDataDTO;


    public OKXFundingRateIndicator(Configuration cfg, String instId) {
        super(cfg);
        this.instId = instId;
    }


    @Override
    public List<Indicator> dependencies() {
        return Collections.emptyList();
    }

    protected Num evaluate(Num num) {
        if (lastDataDTO == null) {
            return refresh(num);
        }
//        判断下期费率时间
        if (lastDataDTO.getNextFundingTime() < System.currentTimeMillis() ||
//              提前更新时间 1小时   每5分钟内 不重复更新
                (lastDataDTO.getNextFundingTime() - System.currentTimeMillis() < advanceTime && System.currentTimeMillis() - lastDataDTO.getTs() > syncTime)) {
            return refresh(num);
        }
        return Num.of(Convert.toDouble(lastFundingRate.multiply(new BigDecimal(100))), num.timestamp());
    }


    /**
     * 刷新费率
     *
     * @param num
     * @return
     */
    private Num refresh(Num num) {
        try {
            //链式构建请求
            String body = HttpRequest.get(RATE_URL + instId)
                    .header(Header.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")//头信息，多个头信息多次调用此方法即可
                    .timeout(20000)//超时，毫秒
                    .execute().body();
            OuYIFundingRateDto rateDto = JSON.parseObject(body, OuYIFundingRateDto.class);
            if (Objects.equals(rateDto.getCode(), "0") && !rateDto.getData().isEmpty()) {
                OuYIFundingRateDto.DataDTO dataDTO = rateDto.getData().getFirst();
                lastDataDTO = dataDTO;
                lastFundingRate = dataDTO.getFundingRate();
            }
        } catch (Exception e) {
            log.error("资金费率请求异常", e);
        }
        return Num.of(Convert.toDouble(lastFundingRate.multiply(new BigDecimal(100))), num.timestamp());
    }
}
