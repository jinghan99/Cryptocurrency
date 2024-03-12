package org.dromara.northstar.gateway.ctp.x64v6v3v15v;

import java.util.Map;
import java.util.stream.Collectors;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.GatewayFactory;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.ctp.CtpGatewayChannelProvider;
import org.dromara.northstar.gateway.ctp.CtpGatewaySettings;
import org.dromara.northstar.gateway.ctp.CtpGatewaySettings.Broker;

import com.alibaba.fastjson.JSON;

public class CtpGatewayFactory implements GatewayFactory{

	private FastEventEngine fastEventEngine;
	
	private IMarketCenter mktCenter;
	
	private Map<String, Broker> brokerMap;
	
	public CtpGatewayFactory(FastEventEngine fastEventEngine, IMarketCenter mktCenter) {
		this.brokerMap = new CtpGatewayChannelProvider().brokerList().stream().collect(Collectors.toMap(Broker::getName, b -> b));
		this.fastEventEngine = fastEventEngine;
		this.mktCenter = mktCenter;
	}
	
	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		CtpGatewaySettings settings = JSON.parseObject(JSON.toJSONString(gatewayDescription.getSettings()), CtpGatewaySettings.class);
		if(!brokerMap.containsKey(settings.getBrokerName())) {
			throw new IllegalStateException("没有找到期货公司信息：" + settings.getBrokerName());
		}
		settings.setBroker(brokerMap.get(settings.getBrokerName()));
		return new CtpGatewayAdapter(fastEventEngine, gatewayDescription.toBuilder().settings(settings).build(), mktCenter);
	}
	
}
