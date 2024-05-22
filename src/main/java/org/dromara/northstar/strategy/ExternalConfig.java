package org.dromara.northstar.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
public class ExternalConfig {

	static {
		log.info("=====================================================");
		log.info("               加载jh-strategy                 ");
		log.info("=====================================================");
	}
}
