package org.dromara.northstar.strategy.indicator;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.*;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.northstar.indicator.AbstractIndicator;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.indicator.model.Num;
import org.dromara.northstar.strategy.dto.OuYIFundingRateDto;

import java.math.BigDecimal;
import java.util.*;

/**
 * 欧意 资金费率 0.15%
 * https://www.okx.com/api/v5/public/funding-rate?instId=BTC-USDT-SWAP
 */
@Slf4j
public class OKXFundingRateIndicator extends AbstractIndicator implements Indicator {


    private static final String RATE_URL = "https://www.okx.com/api/v5/public/funding-rate?instId=";


    //    获取最近3个月的历史资金费率
    private static final String HISTORY_URL = "https://www.okx.com/api/v5/public/funding-rate-history";
    private static final int LIMIT = 100;
    private static final long THREE_MONTHS_IN_MILLISECONDS = 90L * 24 * 60 * 60 * 1000;
    /**
     * 历史费率
     */
    private static final TreeMap<Long, Double> historyRateMap = new TreeMap<>();
    //    是否载入完成
    private static Boolean isReady = false;


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
        history(instId);
    }


    @Override
    public List<Indicator> dependencies() {
        return Collections.emptyList();
    }

    protected Num evaluate(Num num) {
//        等待载入完成
        while (!isReady) {
            ThreadUtil.sleep(1000);
        }
//        判断是否有历史数据 floorEntry 获取最接近的 之前的 一个费率
        Map.Entry<Long, Double> entry = historyRateMap.floorEntry(num.timestamp());
//        不是最新的一个费率 或者 在4小时 以内
        if (!historyRateMap.lastEntry().getKey().equals(entry.getKey())|| num.timestamp() - entry.getKey() < 1000 * 60 * 4) {
            BigDecimal rate = BigDecimal.valueOf(entry.getValue());
            return Num.of(Convert.toDouble(rate.multiply(new BigDecimal(100))), num.timestamp());
        }
        if (lastDataDTO == null) {
            return currentRate(num);
        }
//        判断下期费率时间
        if (lastDataDTO.getNextFundingTime() < System.currentTimeMillis() ||
//              提前更新时间 1小时   每5分钟内 不重复更新
                (lastDataDTO.getNextFundingTime() - System.currentTimeMillis() < advanceTime && System.currentTimeMillis() - lastDataDTO.getTs() > syncTime)) {
            return currentRate(num);
        }
        return Num.of(Convert.toDouble(lastFundingRate), num.timestamp());
    }


    /**
     * 获取当前费率
     *
     * @param num
     * @return
     */
    private Num currentRate(Num num) {
        //链式构建请求
        try {
            String swap = instId.contains("@") ? instId.split("@")[0] : instId;
            String body = HttpRequest.get(RATE_URL + swap)
                    .header(Header.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")//头信息，多个头信息多次调用此方法即可
                    .timeout(20000)//超时，毫秒
                    .execute().body();
            OuYIFundingRateDto rateDto = JSON.parseObject(body, OuYIFundingRateDto.class);
            if (Objects.equals(rateDto.getCode(), "0") && !rateDto.getData().isEmpty()) {
                OuYIFundingRateDto.DataDTO dataDTO = rateDto.getData().getFirst();
                lastDataDTO = dataDTO;
                lastFundingRate = dataDTO.getFundingRate().multiply(new BigDecimal(100));
            }
        } catch (HttpException e) {
            log.error("资金费率请求异常", e);
        }
        return Num.of(Convert.toDouble(lastFundingRate), num.timestamp());
    }


    /**
     * 获取指定合约ID的最近三个月资金费率
     *
     * @param instId 合约ID
     */
    private void history(String instId) {
        // 当前时间的时间戳（毫秒）
        long currentTime = System.currentTimeMillis();
        // 三个月前的时间戳（毫秒）
        long threeMonthsAgo = currentTime - THREE_MONTHS_IN_MILLISECONDS;
        // 如果instId包含"@", 取其"@"之前的部分作为实际合约ID
        String swap = instId.contains("@") ? instId.split("@")[0] : instId;
        // 初始请求URL
        String url = HISTORY_URL + "?instId=" + swap + "&before=" + threeMonthsAgo + "&limit=" + LIMIT;
        isReady = false;
        // 循环请求，直到获取到三个月前的数据为止
        while (true) {
            String body = null;
            try {
                // 发送HTTP GET请求
                HttpResponse response = HttpRequest.get(url)
                        .header(Header.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                        .timeout(20000)  // 设置请求超时时间为20000毫秒
                        .execute();
                // 获取响应体字符串
                body = response.body();
            } catch (Exception e) {
                ThreadUtil.sleep(1000);
                continue;
            }
            // 解析响应体JSON
            JSONObject jsonResponse = JSONUtil.parseObj(body);
            JSONArray data = jsonResponse.getJSONArray("data");
            // 如果没有数据，则退出循环
            if (data.isEmpty()) {
                break;
            }
            // 遍历每条数据
            for (int i = 0; i < data.size(); i++) {
                JSONObject entry = data.getJSONObject(i);
                long fundingTime = entry.getLong("fundingTime");  // 资金费时间
                Double fundingRate = entry.getDouble("fundingRate");  // 资金费率
                historyRateMap.put(fundingTime, fundingRate);
                BigDecimal rate = new BigDecimal(fundingRate.toString());
                String formattedRate = rate.toPlainString();
                // 将资金费时间和资金费率存入TreeMap
                historyRateMap.put(fundingTime, Double.valueOf(formattedRate));
            }
            // 获取最后一条数据的资金费时间，用于下一次请求分页
            long lastFundingTime = data.getJSONObject(0).getLong("fundingTime");
            // 更新URL，添加before参数
            url = HISTORY_URL + "?instId=" + swap + "&before=" + lastFundingTime + "&limit=" + LIMIT;
            log.info("载入历史费率完成 " + data.size() + " entries from " + url);
        }
        isReady = true;
    }

}
