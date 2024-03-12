package org.dromara.northstar.gateway.ctp;

import java.util.List;

import org.dromara.northstar.common.GatewaySettings;
import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CtpGatewaySettings extends DynamicParams implements GatewaySettings{
	
	@Setting(label = "网关账户", order = 10)
	private String userId;
	
	@Setting(label = "网关密码", type = FieldType.PASSWORD, order = 20)
	private String password;
	
	@Setting(label = "期货公司", type = FieldType.SELECT, optionProvider = CtpGatewayChannelProvider.class, placeholder = "请选择", order = 30)
	private String brokerName;
	
	private Broker broker;
	
	@Getter
	@Setter
	public static class Broker {
		
		private String name;
		
		private String brokerId;

		private String appId;
		
		private int mdPort;
		
		private int tdPort;
		
		private String authCode;
		
		private List<String> hosts;
		
	}
}
