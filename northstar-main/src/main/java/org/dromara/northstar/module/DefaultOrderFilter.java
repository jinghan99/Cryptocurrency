package org.dromara.northstar.module;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.exception.TradeException;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.OrderRequestFilter;
import org.slf4j.Logger;


public class DefaultOrderFilter implements OrderRequestFilter {

	private static final int MAX_ORDER_REQ_PER_DAY = 4000; //按CTP日内免申报费的上限设置
	
	private IModuleContext ctx;
	
	private LocalDate tradingDay;
	
	private Map<String, AtomicInteger> contractReqCounterMap = new HashMap<>();
	
	private Logger logger;
	
	public DefaultOrderFilter(List<ContractSimpleInfo> filterContract, IModuleContext ctx) {
		this.ctx = ctx;
		this.logger = ctx.getLogger(getClass());
		filterContract.forEach(c -> contractReqCounterMap.put(c.getUnifiedSymbol(), new AtomicInteger()));
	}

	@Override
	public void onTick(Tick tick) {
		if(!tradingDay.equals(tick.tradingDay())) {
			tradingDay = tick.tradingDay();
			contractReqCounterMap.values().forEach(cnt -> cnt.set(0));
		}
	}
	

	@Override
	public void doFilter(SubmitOrderReq orderReq) {
		logger.info("默认订单过滤器正进行风控过滤");
		if(!contractReqCounterMap.containsKey(orderReq.contract().unifiedSymbol())) {
			throw new NoSuchElementException(String.format("模组没包含合约：%s。 可选合约：%s", orderReq.contract().unifiedSymbol(), contractReqCounterMap.keySet()));
		}
		String unifiedSymbol = orderReq.contract().unifiedSymbol();
		if(logger.isDebugEnabled()) {
			logger.debug("当天 [{}] 合约的剩余发单次数为：{}", unifiedSymbol, MAX_ORDER_REQ_PER_DAY - contractReqCounterMap.get(unifiedSymbol).get());
		}
		if(contractReqCounterMap.get(unifiedSymbol).getAndIncrement() > MAX_ORDER_REQ_PER_DAY) {
			logger.warn("模组触发 [{}] 合约的日内免费申报上限。自动停用模组。", orderReq.contract().name());
			ctx.setEnabled(false);
			throw new TradeException("触发风控规则，中止委托发单");
		}
	}

}
