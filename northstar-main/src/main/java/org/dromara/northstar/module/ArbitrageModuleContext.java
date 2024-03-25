package org.dromara.northstar.module;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.Tuple;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.dromara.northstar.support.utils.bar.BarMergerRegistry;
import org.slf4j.Logger;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import cn.hutool.core.lang.Assert;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;

public class ArbitrageModuleContext extends ModuleContext implements IModuleContext{
	
	private ExecutorService exec = Executors.newFixedThreadPool(2);
	
	private final Logger logger;

	public ArbitrageModuleContext(TradeStrategy tradeStrategy, ModuleDescription moduleDescription,
			ModuleRuntimeDescription moduleRtDescription, IContractManager contractMgr, IModuleRepository moduleRepo, BarMergerRegistry barMergerRegistry) {
		super(tradeStrategy, moduleDescription, moduleRtDescription, contractMgr, moduleRepo, barMergerRegistry);
		logger = getLogger(getClass());
	}

	/**
	 * 套利模组上下文为了实现同时发单，利用了多线程处理下单过程
	 */
	@Override
	public void submitOrderReq(TradeIntent tradeIntent) {
		exec.execute(() -> {
			logger.info("下单交由子线程处理");
			if(!module.isEnabled()) {
				if(isReady()) {
					logger.info("策略处于停用状态，忽略委托单");
				}
				return;
			}
			mktCenter.lastTick(tradeIntent.getContract()).ifPresentOrElse(tick -> {
				logger.info("收到下单意图：{}", tradeIntent);
				tradeIntentMap.put(tradeIntent.getContract(), tradeIntent);
				tradeIntent.setContext(this);
				tradeIntent.onTick(tick);	
			}, () -> logger.warn("没有TICK行情数据时，忽略下单请求"));
		});
	}

	/**
	 * 套利模组上下文实际下单时，与投机模组上下文的区别在于，少了状态机的拦截，以实现同时下单
	 */
	@Override
	public Optional<String> submitOrderReq(Contract contract, SignalOperation operation, PriceType priceType, int volume, double price) {
		if(!module.isEnabled()) {
			if(isReady()) {
				logger.info("策略处于停用状态，忽略委托单");
			}
			return Optional.empty();
		}
		Assert.isTrue(volume > 0, "下单手数应该为正数。当前为" + volume);
		Tick tick = mktCenter.lastTick(contract).orElseThrow(() -> new IllegalStateException("没有行情时不应该发送订单"));
		
		double orderPrice = priceType.resolvePrice(tick, operation, price);
		logger.info("[{} {}] 策略信号：合约【{}】，操作【{}】，价格【{}】，手数【{}】，类型【{}】", 
				tick.actionDay(), tick.actionTime(),
				contract.unifiedSymbol(), operation.text(), orderPrice, volume, priceType);
		
		String id = UUID.randomUUID().toString();
		String gatewayId = getAccount(contract).accountId();
		DirectionEnum direction = OrderUtils.resolveDirection(operation);
		int factor = FieldUtils.directionFactor(direction);
		double plusPrice = module.getModuleDescription().getOrderPlusTick() * contract.priceTick(); // 超价设置
		Position pos = getAccount(contract).getPosition(OrderUtils.getClosingDirection(direction), contract)
				.orElse(Position.builder().contract(contract).build());
		Tuple<OffsetFlagEnum, Integer> tuple = module.getModuleDescription().getClosingPolicy().resolve(operation, pos, volume);
		if(tuple.t1() == OffsetFlagEnum.OF_CloseToday) {
			Position updatePos = pos.toBuilder().tdFrozen(tuple.t2()).build();
			getAccount(contract).onPosition(updatePos);
		} else if(tuple.t1() == OffsetFlagEnum.OF_CloseYesterday) {
			Position updatePos = pos.toBuilder().ydFrozen(tuple.t2()).build();
			getAccount(contract).onPosition(updatePos);
		}
		SubmitOrderReq orderReq = SubmitOrderReq.builder()
				.originOrderId(id)
				.contract(contract)
				.gatewayId(gatewayId)
				.direction(direction)
				.offsetFlag(tuple.t1())
				.volume(tuple.t2())
				.price(orderPrice + factor * plusPrice)	// 自动加上超价
				.timeCondition(priceType == PriceType.ANY_PRICE ? TimeConditionEnum.TC_IOC : TimeConditionEnum.TC_GFD)
				.orderPriceType(priceType == PriceType.ANY_PRICE ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
				.contingentCondition(ContingentConditionEnum.CC_Immediately)
				.actionTimestamp(System.currentTimeMillis())
				.minVolume(1)
				.build();
		try {
			if(Objects.nonNull(orderReqFilter)) {
				orderReqFilter.doFilter(orderReq);
			}
		} catch (Exception e) {
			logger.error("发单失败。原因：{}", e.getMessage());
			tradeIntentMap.remove(orderReq.contract());
			return Optional.empty();
		}
		logger.info("发单：{}，{}", orderReq.originOrderId(), LocalDateTime.now());
		String originOrderId = module.getAccount(contract).submitOrder(orderReq);
		orderReqMap.put(originOrderId, orderReq);
		return Optional.of(originOrderId);
	}

	/**
	 * 套利模组上下文实际下单时，与投机模组上下文的区别在于，少了状态机的拦截，以实现及时撤单
	 */
	@Override
	public void cancelOrder(String originOrderId) {
		if(!orderReqMap.containsKey(originOrderId)) {
			logger.debug("找不到订单：{}", originOrderId);
			return;
		}
		logger.info("撤单：{}", originOrderId);
		Contract contract = orderReqMap.get(originOrderId).contract();
		module.getAccount(contract).cancelOrder(originOrderId);
	}

	@Override
	public ModuleRuntimeDescription getRuntimeDescription(boolean fullDescription) {
		ModuleRuntimeDescription mrd = super.getRuntimeDescription(fullDescription);
		if(fullDescription) {
			if(contractMap.size() == 2) {
				Iterator<Contract> it = contractMap.values().iterator();
				Contract c1 = it.next();
				Contract c2 = it.next();
				Contract nearMonth = c1.lastTradeDate().isBefore(c2.lastTradeDate()) ? c1 : c2;
				Contract farMonth = c1 == nearMonth ? c2 : c1;
				String combName = String.format("%s-%s", nearMonth.name(), farMonth.name());
				JSONArray combBarArr = new JSONArray();
				JSONArray nearData = mrd.getDataMap().get(nearMonth.name());
				JSONArray farData = mrd.getDataMap().get(farMonth.name());
				if(nearData.size() == farData.size()) {
					for(int i = 0; i < nearData.size(); i++) {
						JSONObject near = nearData.getJSONObject(i);
						JSONObject far = farData.getJSONObject(i);
						combBarArr.add(compute(near, far));
					}
					mrd.getDataMap().put(combName, combBarArr);
				} else {
					logger.warn("近远月数据长度不一致，无法生成价差合约：{} {}", nearData.size(), farData.size());
				}
			} else if (contractMap.size() == 3) {
				List<Contract> contracts = contractMap.values().stream()
						.sorted((a,b) -> a.unifiedSymbol().compareTo(b.unifiedSymbol()))
						.toList();
				Contract nearMonth = contracts.get(0);
				Contract midMonth = contracts.get(1);
				Contract farMonth = contracts.get(2);
				JSONArray combBarArr = new JSONArray();
				JSONArray nearData = mrd.getDataMap().get(nearMonth.name());
				JSONArray midData = mrd.getDataMap().get(midMonth.name());
				JSONArray farData = mrd.getDataMap().get(farMonth.name());
				if(nearData.size() == midData.size() && midData.size() == farData.size()) {
					for(int i = 0; i < nearData.size(); i++) {
						JSONObject near = nearData.getJSONObject(i);
						JSONObject mid = midData.getJSONObject(i);
						JSONObject far = farData.getJSONObject(i);
						combBarArr.add(compute(near, mid, far));
					}
					mrd.getDataMap().put("蝶式价差率", combBarArr);
				} else {
					logger.warn("近中远月数据长度不一致，无法生成价差合约：{} {} {}", nearData.size(), midData.size(), farData.size());
				}
			}
		}
		return mrd;
	}
	
	private JSONObject compute(JSONObject bar1, JSONObject bar2) {
		JSONObject json = new JSONObject();
		json.put("open", (bar1.getDoubleValue("open") - bar2.getDoubleValue("open")) / bar2.getDoubleValue("open") * 100);
		json.put("low", (bar1.getDoubleValue("low") - bar2.getDoubleValue("low")) / bar2.getDoubleValue("low") * 100);
		json.put("high", (bar1.getDoubleValue("high") - bar2.getDoubleValue("high")) / bar2.getDoubleValue("high") * 100);
		json.put("close", (bar1.getDoubleValue("close") - bar2.getDoubleValue("close")) / bar2.getDoubleValue("close") * 100);
		json.put("timestamp", bar1.getLongValue("timestamp"));
		return json;
	}
	
	private JSONObject compute(JSONObject near, JSONObject mid, JSONObject far) {
		JSONObject json = new JSONObject();
		json.put("open", (near.getDoubleValue("open") / mid.getDoubleValue("open") - mid.getDoubleValue("open") / far.getDoubleValue("open")) * 100);
		json.put("low", (near.getDoubleValue("low") / mid.getDoubleValue("low") - mid.getDoubleValue("low") / far.getDoubleValue("low")) * 100);
		json.put("high", (near.getDoubleValue("high") / mid.getDoubleValue("high") - mid.getDoubleValue("high") / far.getDoubleValue("high")) * 100);
		json.put("close", (near.getDoubleValue("close") / mid.getDoubleValue("close") - mid.getDoubleValue("close") / far.getDoubleValue("close")) * 100);
		json.put("timestamp", near.getLongValue("timestamp"));
		return json;
	}
}
