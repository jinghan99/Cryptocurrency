package org.dromara.northstar.gateway.ctp.x64v6v5v1cpv;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.GatewayFactory;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.ctp.CtpSimGatewaySettings;

import com.alibaba.fastjson.JSON;

public class CtpSimGatewayFactory implements GatewayFactory{

	private FastEventEngine fastEventEngine;
	private IMarketCenter mktCenter;

	public CtpSimGatewayFactory(FastEventEngine fastEventEngine, IMarketCenter mktCenter) {
		this.fastEventEngine = fastEventEngine;
		this.mktCenter = mktCenter;
	}

	@Override
	public Gateway newInstance(GatewayDescription gatewayDescription) {
		CtpSimGatewaySettings settings = JSON.parseObject(JSON.toJSONString(gatewayDescription.getSettings()), CtpSimGatewaySettings.class);
		return new CtpSimGatewayAdapter(fastEventEngine, gatewayDescription.toBuilder().settings(settings).build(), mktCenter);
	}

}
