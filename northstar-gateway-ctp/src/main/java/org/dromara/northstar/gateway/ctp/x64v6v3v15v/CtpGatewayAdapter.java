package org.dromara.northstar.gateway.ctp.x64v6v3v15v;

import java.io.File;

import static xyz.redtorch.pb.CoreEnum.ExchangeEnum.INE;
import static xyz.redtorch.pb.CoreEnum.ExchangeEnum.SHFE;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.constant.Platform;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.gateway.ctp.GatewayAbstract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

public class CtpGatewayAdapter extends GatewayAbstract implements MarketGateway, TradeGateway {

	private static final Logger logger = LoggerFactory.getLogger(CtpGatewayAdapter.class);
	
	static {
		String envTmpDir = "";
		String tempLibPath = "";
		try {
			switch(Platform.current()) {
				case WINDOWS -> {
					logger.info("开始复制运行库");
					envTmpDir = System.getProperty("java.io.tmpdir");
					tempLibPath = envTmpDir + File.separator + "xyz" + File.separator + "redtorch" + File.separator + "api" + File.separator + "jctp" + File.separator + "lib" + File.separator
							+ "jctpv6v3v15x64api" + File.separator;
	
					CommonUtils.copyURLToFileForTmp(tempLibPath, CtpGatewayAdapter.class.getResource("/assembly/libiconv.dll"));
					CommonUtils.copyURLToFileForTmp(tempLibPath, CtpGatewayAdapter.class.getResource("/assembly/jctpv6v3v15x64api/thostmduserapi_se.dll"));
					CommonUtils.copyURLToFileForTmp(tempLibPath, CtpGatewayAdapter.class.getResource("/assembly/jctpv6v3v15x64api/thosttraderapi_se.dll"));
					CommonUtils.copyURLToFileForTmp(tempLibPath, CtpGatewayAdapter.class.getResource("/assembly/jctpv6v3v15x64api/jctpv6v3v15x64api.dll"));
					
					logger.info("开始加载运行库");
					System.load(tempLibPath + File.separator + "libiconv.dll");
					System.load(tempLibPath + File.separator + "thostmduserapi_se.dll");
					System.load(tempLibPath + File.separator + "thosttraderapi_se.dll");
					System.load(tempLibPath + File.separator + "jctpv6v3v15x64api.dll");
				}
				case LINUX -> {
					logger.info("开始复制运行库");
					envTmpDir = "/tmp";
					tempLibPath = envTmpDir + File.separator + "xyz" + File.separator + "redtorch" + File.separator + "api" + File.separator + "jctp" + File.separator + "lib" + File.separator
							+ "jctpv6v3v15x64api" + File.separator;
	
					CommonUtils.copyURLToFileForTmp(tempLibPath, CtpGatewayAdapter.class.getResource("/assembly/jctpv6v3v15x64api/libthostmduserapi_se.so"));
					CommonUtils.copyURLToFileForTmp(tempLibPath, CtpGatewayAdapter.class.getResource("/assembly/jctpv6v3v15x64api/libthosttraderapi_se.so"));
					CommonUtils.copyURLToFileForTmp(tempLibPath, CtpGatewayAdapter.class.getResource("/assembly/jctpv6v3v15x64api/libjctpv6v3v15x64api.so"));
					
					logger.info("开始加载运行库");
					System.load(tempLibPath + File.separator + "libthostmduserapi_se.so");
					System.load(tempLibPath + File.separator + "libthosttraderapi_se.so");
					System.load(tempLibPath + File.separator + "libjctpv6v3v15x64api.so");
				}
				default -> throw new IllegalArgumentException("Unexpected value: " + Platform.current());
			}
		} catch (Exception e) {
			logger.warn("运行库加载失败", e);
		}
	}
	
	private MdSpi mdSpi = null;
	private TdSpi tdSpi = null;
	
	public CtpGatewayAdapter(FastEventEngine fastEventEngine, GatewayDescription gd, IMarketCenter mktCenter) {
		super(gd, mktCenter);
		
		switch(gd.getGatewayUsage()){
		case TRADE: 
			tdSpi = new TdSpi(this);
			break;
		case MARKET_DATA:
			mdSpi = new MdSpi(this);
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + gd.getGatewayUsage());
		}
		
		this.fastEventEngine = fastEventEngine;
	}
	
	@Override
	public boolean subscribe(Contract contract) {
		if (gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			if (mdSpi == null) {
				logger.error("{} 行情接口尚未初始化或已断开", logInfo);
				return false;
			}
			return mdSpi.subscribe(contract.symbol());
		}
		logger.warn("{} 不包含订阅功能", logInfo);
		return false;
	}

	@Override
	public boolean unsubscribe(Contract contract) {
		if (gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			if (mdSpi == null) {
				logger.error("{} 行情接口尚未初始化或已断开", logInfo);
				return false;
			}
			return mdSpi.unsubscribe(contract.symbol());
		}
		logger.warn("{} 不包含取消订阅功能", logInfo);
		return false;
	}
	
	@Override
	public String submitOrder(SubmitOrderReq submitOrderReq) {
		if (!isConnected()) {
			throw new IllegalStateException("网关未连线");
		}
		ExchangeEnum exchange = submitOrderReq.contract().exchange();
		ProductClassEnum productClass = submitOrderReq.contract().productClass();
		boolean nonAnyPrice = productClass == ProductClassEnum.FUTURES && (exchange == SHFE || exchange == INE)
				|| productClass == ProductClassEnum.OPTION && exchange != ExchangeEnum.CZCE;
		if(submitOrderReq.orderPriceType() == OrderPriceTypeEnum.OPT_AnyPrice) {
			if(nonAnyPrice) {
				logger.info("{} 交易所不支持{}合约市价单，将自动转换为限价单", exchange, productClass);
				return tdSpi.submitOrder(submitOrderReq.toBuilder().orderPriceType(OrderPriceTypeEnum.OPT_LimitPrice).build());
			}
			logger.info("{} 交易所支持{}合约市价单，将自动转换为市价", exchange, productClass);
			return tdSpi.submitOrder(submitOrderReq.toBuilder().price(0).build());
		}
		
		return tdSpi.submitOrder(submitOrderReq);
	}
	
	@Override
	public boolean cancelOrder(String originOrderId) {
		if (!isConnected()) {
			throw new IllegalStateException("网关未连线");
		}
		return tdSpi.cancelOrder(originOrderId);
	}

	@Override
	public void disconnect() {
		final TdSpi tdSpiForDisconnect = tdSpi;
		final MdSpi mdSpiForDisconnect = mdSpi;
		tdSpi = null;
		mdSpi = null;
		Thread.ofVirtual().unstarted(() -> {
			logger.warn("当前网关类型：{}", gatewayDescription.getGatewayUsage());
			try {
				if(tdSpiForDisconnect != null) {
					tdSpiForDisconnect.disconnect();
					logger.info("断开tdSpi");
				}
				if(mdSpiForDisconnect != null) {
					mdSpiForDisconnect.disconnect();
					logger.info("断开mdSpi");
				}
				logger.warn("{} 异步断开操作完成", logInfo);
			} catch (Throwable t) {
				logger.error(logInfo + " 异步断开操作错误", t);
			}
		}).start();
	}

	@Override
	public void connect() {
		if (gatewayDescription.getGatewayUsage() == GatewayUsage.TRADE) {
			if (tdSpi == null) {
				tdSpi = new TdSpi(this);
			}
			tdSpi.connect();
		} else if (gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA) {
			if (mdSpi == null) {
				mdSpi = new MdSpi(this);
			}
			mdSpi.connect();
		}
	}

	private boolean isConnected() {
		if (gatewayDescription.getGatewayUsage() == GatewayUsage.TRADE && tdSpi != null) {
			return tdSpi.isConnected();
		} else if (gatewayDescription.getGatewayUsage() == GatewayUsage.MARKET_DATA && mdSpi != null) {
			return mdSpi.isConnected();
		}
		return false;
	}
	
	@Override
	public boolean isActive() {
		if(mdSpi == null) {
			return false;
		}
		return mdSpi.isActive();
	}
	
	@Override
	public ChannelType channelType() {
		return ChannelType.CTP;
	}

	@Override
	public ConnectionState getConnectionState() {
		return connState;
	}

}
