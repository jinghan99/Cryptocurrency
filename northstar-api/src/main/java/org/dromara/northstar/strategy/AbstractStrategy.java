package org.dromara.northstar.strategy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.ModuleAccountDescription;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;
import org.slf4j.Logger;

import com.alibaba.fastjson.JSONObject;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractStrategy implements TradeStrategy{
	
	// 模组计算状态
	@Getter
	@Setter
	protected JSONObject storeObject;
	// 模组上下文
	protected IModuleStrategyContext ctx;
	// 处理器，contract -> handler
	protected Map<Contract, TickHandler> tickHandlerMap = new HashMap<>();
	// 处理器，contract -> handler
	protected Map<Contract, BarHandler> barHandlerMap = new HashMap<>();
	// 日志对象
	private Logger logger;
	// 预热K线数据量（该预热数据量与模组的设置并不相等，该属性用于策略内部判断接收了多少数据，而模组的预热设置用于外部投喂了多少数据）
	protected int numOfBarsToPrepare;
	
	private Queue<Bar> barCache = new LinkedList<>();
	
	@Override
	public void onOrder(Order order) {
		// 如果策略不关心订单反馈，可以不重写
	}

	@Override
	public void onTrade(Trade trade) {
		// 如果策略不关心成交反馈，可以不重写
		if(logger.isInfoEnabled()) {
			logger.info("模组成交 [{} {} {} 操作：{}{} {}手 {}]", trade.contract().unifiedSymbol(),
					trade.tradeDate(), trade.tradeTime(), FieldUtils.chn(trade.direction()), FieldUtils.chn(trade.offsetFlag()), 
					trade.volume(), trade.price());
			logger.info("当前模组净持仓：[{}]", ctx.getModuleAccount().getNonclosedNetPosition(trade.contract()));
			logger.info("当前模组状态：{}", ctx.getState());
		}
	}

	/* 该方法不应该被重写，但可以扩展 */
	@Override
	public void setContext(IModuleContext context) {
		ctx = context;
		logger = ctx.getLogger(getClass());
		initIndicators();
		initMultiContractHandler();
	}
	
	public IModuleContext getContext() {
		return (IModuleContext) ctx;
	}
	
	/**
	 * 是否启用
	 * @return
	 */
	public boolean isEnabled() {
		return getContext().isEnabled();
	}
	
	/**
	 * 模组绑定的合约
	 * @return
	 */
	protected List<ContractSimpleInfo> bindedContracts(){
		return getContext().getModule().getModuleDescription().getModuleAccountSettingsDescription()
				.stream()
				.map(ModuleAccountDescription::getBindedContracts)
				.flatMap(List::stream)
				.toList();
	}
	
	/**
	 * 指标初始化
	 */
	protected void initIndicators() {}
	
	/**
	 * 多合约处理器初始化
	 */
	protected void initMultiContractHandler() {}

	/**
	 * 该方法不管模组是否启用都会被调用
	 * 每个TICK触发一次
	 * 如果订阅了多个合约，则会有多个TICK，因此每个TICK时刻会触发多次
	 */
	@Override
	public void onTick(Tick tick) {
		if(!canProceed()) {
			return;
		}
		if(tickHandlerMap.containsKey(tick.contract())) {
			tickHandlerMap.get(tick.contract()).onTick(tick);
		}
	}
	
	/**
	 * 订阅多个合约时，可以加上各自的处理器来减少if...else代码
	 * @param contract
	 * @param handler
	 */
	protected void addTickHandler(Contract contract, TickHandler handler) {
		tickHandlerMap.put(contract, handler);
	}

	/**
	 * 该方法不管模组是否启用都会被调用
	 * 每个K线触发一次
	 * 如果订阅了多个合约，则会有多个K线，因此每个K线时刻会触发多次
	 */
	@Override
	public void onMergedBar(Bar bar) {
		if(!canProceed(bar)) {
			return;
		}
		if(barHandlerMap.containsKey(bar.contract())) {
			barHandlerMap.get(bar.contract()).onMergedBar(bar);
		}
	}
	
	protected boolean canProceed() {
		return barCache.isEmpty() && numOfBarsToPrepare == 0;
	}
	
	protected boolean canProceed(Bar bar) {
		if(barCache.size() < numOfBarsToPrepare) {
			if(barCache.isEmpty() || barCache.peek().contract().equals(bar.contract())) {
				barCache.offer(bar);
			}
			return false;
		}
		
		numOfBarsToPrepare = 0;
		barCache.clear();
		return true;
	}
	
	/**
	 * 订阅多个合约时，可以加上各自的处理器来减少if...else代码
	 * @param contract
	 * @param handler
	 */
	protected void addBarHandler(Contract contract, BarHandler handler) {
		barHandlerMap.put(contract, handler);
	}
	
	protected static interface TickHandler extends TickDataAware {}
	
	protected static interface BarHandler extends MergedBarListener {}
}
