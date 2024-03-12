package org.dromara.northstar.gateway.ctp;

import org.dromara.northstar.common.GatewaySettings;
import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;

import lombok.Getter;
import lombok.Setter;

/**
 * 仿真网关配置，仅用于做穿透测试
 * @author KevinHuangwl
 *
 */
@Getter
@Setter
public class CtpSimGatewaySettings extends DynamicParams implements GatewaySettings {

	@Setting(label = "网关账户", order = 10)
	private String userId;
	
	@Setting(label = "网关密码", type = FieldType.PASSWORD, order = 20)
	private String password;
	
	@Setting(label = "BrokerID", required = false, order = 30)
	private String brokerId = "3070";		//宏源仿真
	
	@Setting(label = "Host", required = false, order = 40)
	private String host = "101.230.79.235";	//宏源仿真

	@Setting(label = "MdPort", required = false, order = 50)
	private String mdPort = "33213";
	
	@Setting(label = "TdPort", required = false, order = 60)
	private String tdPort = "33205";
	
	@Setting(label = "AuthCode", required = false, order = 70)
	private String authCode = "FT2REDDI5RRKK4O7";
	
	@Setting(label = "AppID", required = false, order = 80)
	private String appId = "client_northstar_2.0";
}
