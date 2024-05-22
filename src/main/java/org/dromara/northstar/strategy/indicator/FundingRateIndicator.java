package org.dromara.northstar.strategy.indicator;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.strategy.constant.DirectionEnum;
import org.dromara.northstar.strategy.domain.FixedSizeQueue;
import org.dromara.northstar.strategy.dto.OuYIFundingRateDto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 资金费率
 * https://www.okx.com/api/v5/public/funding-rate?instId=BTC-USDT-SWAP
 */
@Slf4j
public class FundingRateIndicator extends AbstractIndicator implements Indicator {


    private static final String RATE_URL = "https://www.okx.com/api/v5/public/funding-rate?instId=";


    @Getter
    FixedSizeQueue<DirectionEnum> fixedSizeQueue;

    /**
     * 产品ID ，如 BTC-USD-SWAP 仅适用于永续
     */
    private String instId;

    private double lastFundingRate;

    public FundingRateIndicator(Configuration cfg, String instId, int barCount) {
        super(cfg);
        fixedSizeQueue = new FixedSizeQueue<>(barCount * 3);
        this.instId = instId;
    }


    @Override
    public List<Indicator> dependencies() {
        return Collections.emptyList();
    }

    protected Num evaluate(Num num) {
        try {
            //链式构建请求
            String body = HttpRequest.get(RATE_URL + instId)
                    .header(Header.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")//头信息，多个头信息多次调用此方法即可
                    .timeout(20000)//超时，毫秒
                    .execute().body();
            OuYIFundingRateDto rateDto = JSONUtil.toBean(body, OuYIFundingRateDto.class);
            if (Objects.equals(rateDto.getCode(), "0")) {
                lastFundingRate = rateDto.getData().getFirst().getFundingRate();
                return Num.of(lastFundingRate, num.timestamp());
            }
        } catch (Exception e) {
            log.error("资金费率请求异常", e);
        }
        return Num.of(lastFundingRate, num.timestamp());
    }
}
