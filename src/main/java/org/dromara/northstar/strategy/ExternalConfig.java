package org.dromara.northstar.strategy;

import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ExternalConfig {

	static {
		log.info("=====================================================");
		log.info("               加载jh-strategy                 ");
		log.info("=====================================================");
	}
}
