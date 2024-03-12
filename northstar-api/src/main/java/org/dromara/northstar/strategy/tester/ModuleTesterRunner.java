package org.dromara.northstar.strategy.tester;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "spring.profiles", name = "active", havingValue = "test")
@Component
public class ModuleTesterRunner implements CommandLineRunner {

	@Autowired
	AbstractTester tester;

	@Override
	public void run(String... args) throws Exception {
		new Thread(() -> {
			log.info("模组自动化测试准备开始");
			try {
				Thread.sleep(5000);
				tester.start();
			} catch (InterruptedException e) {
				log.warn("", e);
			}
			
			log.info("模组自动化测试结束");
		}).start();
	}
}
