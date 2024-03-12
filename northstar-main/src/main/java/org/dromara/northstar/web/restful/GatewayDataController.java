package org.dromara.northstar.web.restful;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.List;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.ResultBean;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.utils.MarketDataLoadingUtils;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IContractManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import xyz.redtorch.pb.CoreField.BarField;

@RequestMapping("/northstar/data")
@RestController
public class GatewayDataController {

	@Autowired
	private IMarketDataRepository mdRepo;
	
	@Autowired
	private IGatewayRepository gatewayRepo;
	
	@Autowired
	private IContractManager contractMgr;
	
	private MarketDataLoadingUtils utils = new MarketDataLoadingUtils();
	
	@GetMapping("/bar/min")
	public ResultBean<List<byte[]>> loadWeeklyBarData(String gatewayId, String unifiedSymbol, long refStartTimestamp, boolean firstLoad){
		Assert.notNull(unifiedSymbol, "合约代码不能为空");
		GatewayDescription gd = gatewayRepo.findById(gatewayId);
		if(gd.getChannelType() == ChannelType.PLAYBACK || gd.getChannelType() == ChannelType.SIM) {
			return new ResultBean<>(Collections.emptyList());
		}
		IContract contract = contractMgr.getContract(gd.getChannelType(), unifiedSymbol);
		LocalDate start = utils.getFridayOfLastWeek(refStartTimestamp);
		if(firstLoad && Period.between(start, LocalDate.now()).getDays() < 7) {
			start = start.minusWeeks(1);
		}
		LocalDate end = utils.getCurrentTradeDay(refStartTimestamp, firstLoad);
		List<Bar> result = Collections.emptyList();
		for(int i=0; i<3; i++) {
			result = mdRepo.loadBars(contract, start.minusWeeks(i), end.minusWeeks(i));
			if(!result.isEmpty()) {
				break;
			}
		}
		
		return new ResultBean<>(result.stream()
				.map(Bar::toBarField)
				.map(BarField::toByteArray)
				.toList());
	}
	
}
