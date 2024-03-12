package org.dromara.northstar.web.restful;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.GatewayTypeDescription;
import org.dromara.northstar.common.model.ResultBean;
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.support.utils.NamingValidator;
import org.dromara.northstar.web.service.GatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/northstar/gateway")
@RestController
public class GatewayManagementController {
	
	@Autowired
	GatewayMetaProvider gatewayMetaProvider;
	
	@Autowired
	protected GatewayService gatewayService;
	
	@Autowired
	protected IContractManager contractMgr;
	
	private NamingValidator validator = new NamingValidator();
	
	@PostMapping
	public ResultBean<Boolean> create(@RequestBody GatewayDescription gd) throws Exception {
		Assert.notNull(gd, "网关配置对象不能为空");
		Assert.isTrue(validator.isValid(gd.getGatewayId()), "网关名称仅支持中英文、数字、及下划线等字符");
		return new ResultBean<>(gatewayService.createGateway(gd));
	}
	
	@DeleteMapping
	public ResultBean<Boolean> remove(String gatewayId) {
		Assert.hasText(gatewayId, "网关ID不能为空");
		return new ResultBean<>(gatewayService.deleteGateway(gatewayId));
	}
	
	@PutMapping
	public ResultBean<Boolean> modify(@RequestBody GatewayDescription gd) throws Exception {
		Assert.notNull(gd, "网关配置对象不能为空");
		Assert.isTrue(validator.isValid(gd.getGatewayId()), "网关名称仅支持中英文、数字、及下划线等字符");
		return new ResultBean<>(gatewayService.updateGateway(gd));
	}
	
	@GetMapping
	public ResultBean<List<GatewayDescription>> list(String usage) { 
		if(StringUtils.isBlank(usage)) {
			return new ResultBean<>(gatewayService.findAllGatewayDescription());
		}
		if(GatewayUsage.valueOf(usage) == GatewayUsage.MARKET_DATA) {
			return new ResultBean<>(gatewayService.findAllMarketGatewayDescription());
		}
		return new ResultBean<>(gatewayService.findAllTraderGatewayDescription());
	}
	
	@GetMapping("/specific")
	public ResultBean<GatewayDescription> findGatewayDescription(String gatewayId){
		Assert.hasText(gatewayId, "网关ID不能为空");
		return new ResultBean<>(gatewayService.findGatewayDescription(gatewayId));
	}
	
	@GetMapping("/active")
	public ResultBean<Boolean> getGatewayActive(String gatewayId){
		Assert.hasText(gatewayId, "网关ID不能为空");
		return new ResultBean<>(gatewayService.isActive(gatewayId));
	}
	
	@GetMapping("/connection")
	public ResultBean<Boolean> connect(String gatewayId) {
		Assert.hasText(gatewayId, "网关ID不能为空");
		return new ResultBean<>(gatewayService.connect(gatewayId));
	}
	
	@DeleteMapping("/connection")
	public ResultBean<Boolean> disconnect(String gatewayId) {
		Assert.hasText(gatewayId, "网关ID不能为空");
		return new ResultBean<>(gatewayService.disconnect(gatewayId));
	}
	
	@PostMapping("/moneyio")
	public ResultBean<Boolean> simMoneyIO(String gatewayId, int money){
		Assert.hasText(gatewayId, "网关ID不能为空");
		return new ResultBean<>(gatewayService.simMoneyIO(gatewayId, money));
	}
	
	@GetMapping("/settings")
	public ResultBean<Collection<ComponentField>> getGatewaySettingsMetaInfo(ChannelType channelType){
		return new ResultBean<>(gatewayService.getGatewaySettingsMetaInfo(channelType));
	}
	
	@GetMapping("/types")
	public ResultBean<Collection<GatewayTypeDescription>> gatewayTypeOptions(){
		return new ResultBean<>(gatewayMetaProvider.availableChannel().stream().map(GatewayTypeDescription::new).toList());
	}
	
	@GetMapping("/reset")
	public ResultBean<Boolean> resetPlayback(String gatewayId) throws Exception{
		Assert.hasText(gatewayId, "网关ID不能为空");
		return new ResultBean<>(gatewayService.resetPlayback(gatewayId));
	}
}
