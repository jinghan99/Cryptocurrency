package org.dromara.northstar.gateway.ctp.x64v6v3v15v;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.common.utils.TradeTimeUtil;
import org.dromara.northstar.gateway.ctp.CtpGatewaySettings;
import org.dromara.northstar.gateway.ctp.GatewayAbstract;
import org.dromara.northstar.gateway.ctp.SmartGatewayConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AtomicDouble;

import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcDepthMarketDataField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcForQuoteRspField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcMdApi;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcMdSpi;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcReqUserLoginField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcRspInfoField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcRspUserLoginField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcSpecificInstrumentField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcUserLogoutField;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;

public class MdSpi extends CThostFtdcMdSpi {

	private static final Logger logger = LoggerFactory.getLogger(MdSpi.class);
	
	private GatewayAbstract gatewayAdapter;
	private String logInfo;
	private String gatewayId;
	private CtpGatewaySettings settings;
	private LocalDate tradingDay;

	private volatile long lastUpdateTickTime = System.currentTimeMillis();

	private ConcurrentMap<TradeTimeDefinition, TradeTimeUtil> tradeTimeBitmap = new ConcurrentHashMap<>();
	private Set<String> subscribedSymbolSet = ConcurrentHashMap.newKeySet();
	
	private ExecutorService exec;
	
	MdSpi(GatewayAbstract gatewayAdapter) {
		this.gatewayAdapter = gatewayAdapter;
		this.gatewayId = gatewayAdapter.gatewayId();
		this.settings = (CtpGatewaySettings) gatewayAdapter.gatewayDescription().getSettings();
		this.logInfo = "行情网关ID-[" + this.gatewayId + "] [→] ";
		if(logger.isInfoEnabled()) {
			logger.info("当前MdApi版本号：{}", CThostFtdcMdApi.GetApiVersion());
		}
	}

	private CThostFtdcMdApi cThostFtdcMdApi;

	private boolean loginStatus = false; // 登陆状态

	public void connect() {
		if (isConnected() || gatewayAdapter.getConnectionState() == ConnectionState.CONNECTING) {
			return;
		}

		if (gatewayAdapter.getConnectionState() == ConnectionState.CONNECTED) {
			login();
			return;
		}
		
		exec = CommonUtils.newThreadPerTaskExecutor(getClass());
		gatewayAdapter.setConnectionState(ConnectionState.CONNECTING);
		loginStatus = false;

		if (cThostFtdcMdApi != null) {
			try {
				logger.warn("{}行情接口检测到旧实例,准备释放", logInfo);
				CThostFtdcMdApi cThostFtdcMdApiForRelease = cThostFtdcMdApi;
				cThostFtdcMdApi = null;
				exec.execute(() -> {
					cThostFtdcMdApiForRelease.RegisterSpi(null);
					Thread.currentThread().setName("GatewayId [" + gatewayId + "] MD API Release Thread, Start Time " + System.currentTimeMillis());
					try {
						logger.warn("行情接口异步释放启动！");
						cThostFtdcMdApiForRelease.Release();
						logger.warn("行情接口异步释放完成！");
					} catch (Throwable t) {
						logger.error("行情接口异步释放发生异常！", t);
					}
				});
			} catch (Throwable t) {
				logger.warn("{}交易接口连接前释放异常", logInfo, t);
			}
		}

		logger.warn("{}行情接口实例初始化", logInfo);

		String envTmpDir = System.getProperty("java.io.tmpdir");
		String tempFilePath = envTmpDir + File.separator + "xyz" + File.separator + "redtorch" + File.separator + "gateway" + File.separator + "ctp" + File.separator + "jctpv6v3v15x64api"
				+ File.separator + "CTP_FLOW_TEMP" + File.separator + "MD_" + this.gatewayId;
		File tempFile = new File(tempFilePath);
		if (!tempFile.getParentFile().exists()) {
			try {
				FileUtils.forceMkdirParent(tempFile);
				logger.info("{}行情接口创建临时文件夹:{}", logInfo, tempFile.getParentFile().getAbsolutePath());
			} catch (IOException e) {
				logger.error("{}行情接口创建临时文件夹失败", logInfo, e);
			}
		}

		logger.warn("{}行情接口使用临时文件夹:{}", logInfo, tempFile.getParentFile().getAbsolutePath());
		
		exec.execute(() -> {			
			try {
				SmartGatewayConnector smartConnector = new SmartGatewayConnector(settings.getBroker().getHosts());
				String mdHost = smartConnector.bestEndpoint();
				int mdPort = settings.getBroker().getMdPort();
				logger.info("使用IP [{}:{}] 连接行情网关", mdHost, mdPort);
				cThostFtdcMdApi = CThostFtdcMdApi.CreateFtdcMdApi(tempFile.getAbsolutePath());
				cThostFtdcMdApi.RegisterSpi(this);
				cThostFtdcMdApi.RegisterFront("tcp://" + mdHost + ":" + mdPort);
				cThostFtdcMdApi.Init();
			} catch (Throwable t) {
				logger.error("{}行情接口连接异常", logInfo, t);
			}
		});

		exec.execute(() -> {
			try {
				Thread.sleep(15000);
				if (!isConnected()) {
					logger.error("{}行情接口连接超时,尝试断开", logInfo);
					gatewayAdapter.disconnect();
				}

			} catch (Throwable t) {
				logger.error("{}行情接口处理连接超时线程异常", logInfo, t);
			}
		});
	}

	// 关闭
	public void disconnect() {
		if (cThostFtdcMdApi != null && gatewayAdapter.getConnectionState() != ConnectionState.DISCONNECTING) {
			logger.warn("{}行情接口实例开始关闭并释放", logInfo);
			loginStatus = false;
			gatewayAdapter.setConnectionState(ConnectionState.DISCONNECTING);
			try {
				CThostFtdcMdApi cThostFtdcMdApiForRelease = cThostFtdcMdApi;
				cThostFtdcMdApi = null;
				exec.execute(() -> {
					cThostFtdcMdApiForRelease.RegisterSpi(null);
					try {
						logger.warn("行情接口异步释放启动！");
						cThostFtdcMdApiForRelease.Release();
						logger.warn("行情接口异步释放完成！");
					} catch (Throwable t) {
						logger.error("行情接口异步释放发生异常", t);
					}
				});
			} catch (Throwable t) {
				logger.error("{}行情接口实例关闭并释放异常", logInfo, t);
			}
			gatewayAdapter.setConnectionState(ConnectionState.DISCONNECTED);
			gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.LOGGED_OUT, gatewayId);
			exec.close();
			logger.warn("{}行情接口实例关闭并释放", logInfo);
		} else {
			logger.warn("{}行情接口实例不存在,无需关闭释放", logInfo);
		}

	}
	
	// 返回接口状态
	public boolean isConnected() {
		return gatewayAdapter.getConnectionState() == ConnectionState.CONNECTED && loginStatus;
	}

	// 订阅行情
	public boolean subscribe(String symbol) {
		logger.debug("订阅合约：{}", symbol);
		subscribedSymbolSet.add(symbol);
		if (isConnected()) {
			try {
				cThostFtdcMdApi.SubscribeMarketData(new String[]{symbol}, 1);
			} catch (Throwable t) {
				logger.error("{}订阅行情异常,合约代码{}", logInfo, symbol, t);
				return false;
			}
			return true;
		} else {
			logger.warn("{}无法订阅行情,行情服务器尚未连接成功,合约代码:{}", logInfo, symbol);
			return false;
		}
	}

	// 退订行情
	public boolean unsubscribe(String symbol) {
		subscribedSymbolSet.remove(symbol);
		if (isConnected()) {
			try {
				cThostFtdcMdApi.UnSubscribeMarketData(new String[]{symbol}, 1);
			} catch (Throwable t) {
				logger.error("{}行情退订异常,合约代码{}", logInfo, symbol, t);
				return false;
			}
			return true;
		} else {
			logger.warn("{}行情退订无效,行情服务器尚未连接成功,合约代码:{}", logInfo, symbol);
			return false;
		}
	}

	private void login() {
		try {
			// 登录
			CThostFtdcReqUserLoginField userLoginField = new CThostFtdcReqUserLoginField();
			userLoginField.setBrokerID(settings.getBroker().getBrokerId());
			userLoginField.setUserID(settings.getUserId());
			userLoginField.setPassword(settings.getPassword());
			cThostFtdcMdApi.ReqUserLogin(userLoginField, 0);
		} catch (Throwable t) {
			logger.error("{}登录异常", logInfo, t);
		}

	}

	// 前置机联机回报
	@Override
	public void OnFrontConnected() {
		try {
			logger.info("{} 行情接口前置机已连接", logInfo);
			// 修改前置机连接状态
			gatewayAdapter.setConnectionState(ConnectionState.CONNECTED);
			login();
			
		} catch (Throwable t) {
			logger.error("{} OnFrontConnected Exception", logInfo, t);
		}
	}

	// 前置机断开回报
	@Override
	public void OnFrontDisconnected(int nReason) {
		try {
			logger.warn("{}行情接口前置机已断开, 原因:{}", logInfo, nReason);
			gatewayAdapter.disconnect();
			gatewayAdapter.setConnectionState(ConnectionState.DISCONNECTED);
			gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.LOGGED_OUT, gatewayId);
		} catch (Throwable t) {
			logger.error("{} OnFrontDisconnected Exception", logInfo, t);
		}
	}

	// 登录回报
	@Override
	public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			if (pRspInfo.getErrorID() == 0) {
				logger.info("{}OnRspUserLogin TradingDay:{},SessionID:{},BrokerId:{},UserID:{}", logInfo, pRspUserLogin.getTradingDay(), pRspUserLogin.getSessionID(), pRspUserLogin.getBrokerID(),
						pRspUserLogin.getUserID());
				if(StringUtils.isBlank(pRspUserLogin.getTradingDay())) {
					logger.warn("没有交易日信息，很可能是由于不在交易时段，将主动断开");
					disconnect();
					return;
				}
				tradingDay = LocalDate.parse(pRspUserLogin.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
				// 修改登录状态为true
				this.loginStatus = true;
				logger.info("{}行情接口获取到的交易日为{}", logInfo, tradingDay);

				if (!subscribedSymbolSet.isEmpty()) {
					String[] symbolArray = subscribedSymbolSet.toArray(new String[subscribedSymbolSet.size()]);
					cThostFtdcMdApi.SubscribeMarketData(symbolArray, subscribedSymbolSet.size());
				}
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.LOGGED_IN, gatewayId);
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.GATEWAY_READY, gatewayId);
			} else {
				logger.warn("{}行情接口登录回报错误 错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
				// 不合法的登录
				if (pRspInfo.getErrorID() == 3) {
					gatewayAdapter.setAuthErrorFlag(true);
				}
			}

		} catch (Throwable t) {
			logger.error("{} OnRspUserLogin Exception", logInfo, t);
		}

	}

	// 心跳警告
	@Override
	public void OnHeartBeatWarning(int nTimeLapse) {
		logger.warn("{} 行情接口心跳警告 nTimeLapse: {}", logInfo, nTimeLapse);
	}

	// 登出回报
	@Override
	public void OnRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {

			if (pRspInfo.getErrorID() != 0) {
				logger.error("{}OnRspUserLogout!错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
			} else {
				logger.warn("{}OnRspUserLogout!BrokerId:{},UserID:{}", logInfo, pUserLogout.getBrokerID(), pUserLogout.getUserID());

			}

		} catch (Throwable t) {
			logger.error("{} OnRspUserLogout Exception", logInfo, t);
		}

		this.loginStatus = false;
	}

	// 错误回报
	@Override
	public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		if (pRspInfo != null) {
			logger.error("{}行情接口错误回报!错误ID:{},错误信息:{},请求ID:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg(), nRequestID);
		} else {
			logger.error("{}行情接口错误回报!不存在错误回报信息", logInfo);
		}
	}

	// 订阅合约回报
	@Override
	public void OnRspSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		if (pRspInfo != null) {
			if (pRspInfo.getErrorID() == 0) {
				if (pSpecificInstrument != null) {
					logger.debug("{}行情接口订阅合约成功:{}", logInfo, pSpecificInstrument.getInstrumentID());
				} else {
					logger.error("{}行情接口订阅合约成功,不存在合约信息", logInfo);
				}
			} else {
				logger.error("{}行情接口订阅合约失败,错误ID:{} 错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
			}
		} else {
			logger.info("{}行情接口订阅回报，不存在回报信息", logInfo);
		}
	}

	// 退订合约回报
	@Override
	public void OnRspUnSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		if (pRspInfo != null) {
			if (pRspInfo.getErrorID() == 0) {
				if (pSpecificInstrument != null) {
					logger.debug("{}行情接口退订合约成功:{}", logInfo, pSpecificInstrument.getInstrumentID());
				} else {
					logger.error("{}行情接口退订合约成功,不存在合约信息", logInfo);
				}
			} else {
				logger.error("{}行情接口退订合约失败,错误ID:{} 错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
			}
		} else {
			logger.info("{}行情接口退订回报，不存在回报信息", logInfo);
		}
	}
	
	private static final LocalTime NIGHT_CUTOFF = LocalTime.of(16, 0);
	private static final LocalTime DAY_OPEN = LocalTime.of(8, 58);
	
	// 合约行情推送
	@Override
	public void OnRtnDepthMarketData(CThostFtdcDepthMarketDataField pDepthMarketData) {
		if (pDepthMarketData == null) {
			logger.warn("{}行情接口收到行情数据为空", logInfo);
			return;
		}
		try {
			String symbol = pDepthMarketData.getInstrumentID();
			
			Contract contract = gatewayAdapter.mktCenter.getContract(ChannelType.CTP, symbol).contract();
			
			LocalTime now = LocalTime.now();
			LocalTime earlyOffset = now.minusMinutes(2);	// 前移两分钟，可覆盖开市集合竞价时段及容忍服务器时间差
			LocalTime lateOffset = now.plusMinutes(1);		// 后移一分钟，容忍服务器时间差
			TradeTimeDefinition ttd = contract.contractDefinition().tradeTimeDef();
			tradeTimeBitmap.computeIfAbsent(ttd, k -> new TradeTimeUtil(ttd));
			TradeTimeUtil util = tradeTimeBitmap.get(ttd);
			TickType tickType = TickType.MARKET_TICK;
			if(!util.withinTradeTime(now) && !util.withinTradeTime(earlyOffset) && !util.withinTradeTime(lateOffset)) {
				tickType = TickType.INFO_TICK;
			}

			LocalDate actionDay = LocalDate.parse(pDepthMarketData.getActionDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
			LocalTime actionTime = LocalTime.parse(pDepthMarketData.getUpdateTime(), DateTimeConstant.T_FORMAT_FORMATTER);
			int updateMillisec = pDepthMarketData.getUpdateMillisec();
			/*
			 * 大商所获取的ActionDay可能是不正确的,因此这里采用本地时间修正 1.请注意，本地时间应该准确 2.使用 SimNow 7x24
			 * 服务器获取行情时,这个修正方式可能会导致问题
			 */
			// 只修正夜盘
			if (contract.exchange() == ExchangeEnum.DCE && actionTime.isAfter(NIGHT_CUTOFF)) {
				actionDay = LocalDate.now();	// 由于大商所没有跨日合约，不用考虑本地时间的误差可能导致的计算问题
			}
			// 修正日盘开盘前INFO_TICK的时间，比如对于大商所而言，INFO_TICK的时间错误会导致指数合约无法生成
			if (tickType == TickType.INFO_TICK && LocalTime.now().isBefore(DAY_OPEN)) {
				Optional<Tick> tickOpt = gatewayAdapter.mktCenter.lastTick(contract);
				if(tickOpt.isPresent()) {
					actionDay = tickOpt.get().actionDay();
					actionTime = tickOpt.get().actionTime();
				} else {
					int offsetDays = LocalDate.now().getDayOfWeek() == DayOfWeek.MONDAY ? 3 : 1;
					actionDay = LocalDate.now().minusDays(offsetDays);
				}
			}
			
			double lastPrice = pDepthMarketData.getLastPrice();
			long volume = pDepthMarketData.getVolume();	//该成交量为当日累计值
			AtomicLong volumeDelta = new AtomicLong();

			Double turnover = pDepthMarketData.getTurnover();	//该金额为当日累计值
			AtomicDouble turnoverDelta = new AtomicDouble();

			Long preOpenInterest = (long) pDepthMarketData.getPreOpenInterest();

			double openInterest = pDepthMarketData.getOpenInterest();
			AtomicDouble openInterestDelta = new AtomicDouble();
			gatewayAdapter.mktCenter.lastTick(contract).ifPresent(tick -> {
				volumeDelta.set(Math.max(0, volume - tick.volume()));	//防止数据异常时为负数
				turnoverDelta.set(turnover - tick.turnover());
				openInterestDelta.set(openInterest - tick.openInterest());
			});

			Double preClosePrice = pDepthMarketData.getPreClosePrice();
			Double preSettlePrice = pDepthMarketData.getPreSettlementPrice();
			Double openPrice = pDepthMarketData.getOpenPrice();
			Double highPrice = pDepthMarketData.getHighestPrice();
			Double lowPrice = pDepthMarketData.getLowestPrice();
			Double upperLimit = pDepthMarketData.getUpperLimitPrice();
			Double lowerLimit = pDepthMarketData.getLowerLimitPrice();

			Double bid1 = parseNumber(pDepthMarketData.getBidPrice1());
			Double bid2 = parseNumber(pDepthMarketData.getBidPrice2());
			Double bid3 = parseNumber(pDepthMarketData.getBidPrice3());
			Double bid4 = parseNumber(pDepthMarketData.getBidPrice4());
			Double bid5 = parseNumber(pDepthMarketData.getBidPrice5());
			
			Double ask1 = parseNumber(pDepthMarketData.getAskPrice1());
			Double ask2 = parseNumber(pDepthMarketData.getAskPrice2());
			Double ask3 = parseNumber(pDepthMarketData.getAskPrice3());
			Double ask4 = parseNumber(pDepthMarketData.getAskPrice4());
			Double ask5 = parseNumber(pDepthMarketData.getAskPrice5());
			
			List<Integer> bidVolumeList = new ArrayList<>();
			bidVolumeList.add(pDepthMarketData.getBidVolume1());
			bidVolumeList.add(pDepthMarketData.getBidVolume2());
			bidVolumeList.add(pDepthMarketData.getBidVolume3());
			bidVolumeList.add(pDepthMarketData.getBidVolume4());
			bidVolumeList.add(pDepthMarketData.getBidVolume5());

			List<Integer> askVolumeList = new ArrayList<>();
			askVolumeList.add(pDepthMarketData.getAskVolume1());
			askVolumeList.add(pDepthMarketData.getAskVolume2());
			askVolumeList.add(pDepthMarketData.getAskVolume3());
			askVolumeList.add(pDepthMarketData.getAskVolume4());
			askVolumeList.add(pDepthMarketData.getAskVolume5());

			Double averagePrice = pDepthMarketData.getAveragePrice();
			Double settlePrice = pDepthMarketData.getSettlementPrice();

			Tick tick = Tick.builder()
					.contract(contract)
					.tradingDay(tradingDay)
					.actionDay(actionDay)
					.actionTime(actionTime)
					.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(actionDay, actionTime).withNano(updateMillisec * 1000000)))
					.avgPrice(isReasonable(upperLimit, lowerLimit, averagePrice) ? averagePrice : preClosePrice)
					.highPrice(isReasonable(upperLimit, lowerLimit, highPrice) ? highPrice : preClosePrice)
					.lowPrice(isReasonable(upperLimit, lowerLimit, lowPrice) ? lowPrice : preClosePrice)
					.openPrice(isReasonable(upperLimit, lowerLimit, openPrice) ? openPrice : preClosePrice)
					.lastPrice(lastPrice)
					.settlePrice(isReasonable(upperLimit, lowerLimit, settlePrice) ? settlePrice : preSettlePrice)
					.openInterest(openInterest)
					.openInterestDelta(openInterestDelta.get())
					.volume(volume)
					.volumeDelta(isReasonable(volume, 0, volumeDelta.get()) ? volumeDelta.get() : 0)
					.turnover(turnover)
					.turnoverDelta(turnoverDelta.get())
					.lowerLimit(lowerLimit)
					.upperLimit(upperLimit)
					.preClosePrice(preClosePrice)
					.preSettlePrice(preSettlePrice)
					.preOpenInterest(preOpenInterest)
					.askPrice(List.of(ask1, ask2, ask3, ask4, ask5))
					.askVolume(askVolumeList)
					.bidPrice(List.of(bid1, bid2, bid3, bid4, bid5))
					.bidVolume(bidVolumeList)
					.gatewayId(gatewayId)
					.channelType(ChannelType.CTP)
					.type(tickType)
					.build();
			
			gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.TICK, tick);
			gatewayAdapter.mktCenter.onTick(tick);
			lastUpdateTickTime = System.currentTimeMillis();
			
		} catch (Throwable t) {
			logger.error("{} OnRtnDepthMarketData Exception", logInfo, t);
		}
	}
	
	private Double parseNumber(double v) {
		if(v == 0D || v == Double.MAX_VALUE) {
			return Double.NaN;
		}
		return v;
	}
	
	private boolean isReasonable(double upperLimit, double lowerLimit, double actual) {
		return upperLimit >= actual && actual >= lowerLimit;
	}

	// 订阅期权询价
	@Override
	public void OnRspSubForQuoteRsp(CThostFtdcSpecificInstrumentField pSpecificInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		logger.info("{}OnRspSubForQuoteRsp", logInfo);
	}

	// 退订期权询价
	@Override
	public void OnRspUnSubForQuoteRsp(CThostFtdcSpecificInstrumentField pSpecificInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		logger.info("{}OnRspUnSubForQuoteRsp", logInfo);
	}

	// 期权询价推送
	@Override
	public void OnRtnForQuoteRsp(CThostFtdcForQuoteRspField pForQuoteRsp) {
		logger.info("{}OnRspUnSubForQuoteRsp", logInfo);
	}
	
	public boolean isActive() {
		return System.currentTimeMillis() - lastUpdateTickTime < 1000;
	}

}