package org.dromara.northstar.gateway.ctp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class SmartGatewayConnectorTest {

	@Test
	void testOneEndpoint() {
		SmartGatewayConnector sgc = new SmartGatewayConnector(List.of("180.166.25.24"));
		assertThat(sgc.bestEndpoint()).isEqualTo("180.166.25.24");
	}

	@Test
	void testManyEndpoint() {
		SmartGatewayConnector sgc = new SmartGatewayConnector(List.of(
				"180.169.112.52",
				  "180.169.112.53",
				  "180.169.112.54",
				  "180.169.112.55",
				"180.166.25.24",
				  "114.80.55.93",
				  "114.80.55.94",
				  "61.186.254.135"));
		assertThat(sgc.bestEndpoint()).isIn(List.of(
				"180.169.112.52",
				  "180.169.112.53",
				  "180.169.112.54",
				  "180.169.112.55",
				"180.166.25.24",
				  "114.80.55.93",
				  "114.80.55.94",
				  "61.186.254.135"));
	}

}
