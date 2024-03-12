package org.dromara.northstar.gateway.ctp;

import java.time.LocalDate;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.ctp.x64v6v3v15v.CtpGatewayFactory;
import org.dromara.northstar.gateway.ctp.x64v6v5v1cpv.CtpSimGatewayFactory;
import org.dromara.northstar.gateway.mktdata.NorthstarDataServiceDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(0)	// 加载顺序需要显式声明，否则会最后才被加载，从而导致加载网关与模组时报异常
@Component
public class CtpLoader implements CommandLineRunner{
	
	@Autowired
	private IMarketCenter mktCenter;
	
	@Autowired
	private NorthstarDataServiceDataSource dsMgr;
	
	@Autowired
	private GatewayMetaProvider gatewayMetaProvider;
	
	@Autowired
	private CtpGatewayFactory ctpFactory;
	
	@Autowired
	private CtpSimGatewayFactory ctpSimFactory;
	
	@Autowired
	private CtpContractDefProvider contractDefPvd;
	
	@Override
	public void run(String... args) throws Exception {
		gatewayMetaProvider.add(ChannelType.CTP, new CtpGatewaySettings(), ctpFactory);
		gatewayMetaProvider.add(ChannelType.CTP_SIM, new CtpSimGatewaySettings(), ctpSimFactory);
		
		mktCenter.addDefinitions(contractDefPvd.get());
		final LocalDate today = LocalDate.now();
		// 加载CTP合约
		dsMgr.getUserAvailableExchanges()
			.stream()
			.forEach(exchange -> {
				dsMgr.getAllContracts(exchange).stream()
					//过滤掉过期合约
				.filter(contract -> contract.lastTradeDate().isAfter(today))
					.forEach(contract -> mktCenter.addInstrument(new CtpContract(contract, dsMgr)));
				log.info("预加载 [{}] 交易所合约信息", exchange);
			});
		mktCenter.loadContractGroup(ChannelType.CTP);
	}

}
