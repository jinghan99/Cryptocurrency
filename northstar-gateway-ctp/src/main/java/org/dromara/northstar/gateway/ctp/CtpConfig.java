package org.dromara.northstar.gateway.ctp;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.ctp.x64v6v3v15v.CtpGatewayFactory;
import org.dromara.northstar.gateway.ctp.x64v6v5v1cpv.CtpSimGatewayFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class CtpConfig {
	
	static {
		log.info("=====================================================");
		log.info("                  加载gateway-ctp                    ");
		log.info("=====================================================");
	}

	@Bean
	CtpGatewayFactory ctpGatewayFactory(FastEventEngine feEngine, IMarketCenter mktCenter) {
		return new CtpGatewayFactory(feEngine, mktCenter);
	}
	
	@Bean
	CtpSimGatewayFactory ctpSimGatewayFactory(FastEventEngine feEngine, IMarketCenter mktCenter) {
		return new CtpSimGatewayFactory(feEngine, mktCenter);
	}
}
