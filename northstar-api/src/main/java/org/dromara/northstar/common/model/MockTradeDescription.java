package org.dromara.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

/**
 * 手工模拟成交
 * @author KevinHuangwl
 *
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MockTradeDescription {

	private String contractId;

	private String gatewayId;
	
	private double price;
	
	private int volume;
	
	private DirectionEnum direction;
	
	private OffsetFlagEnum offsetFlag;

	private String tradeDate;
}
