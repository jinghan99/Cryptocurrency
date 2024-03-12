package org.dromara.northstar.gateway.ctp.x64v6v3v15v;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Notice;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.gateway.ctp.CtpContract;
import org.dromara.northstar.gateway.ctp.CtpGatewaySettings;
import org.dromara.northstar.gateway.ctp.GatewayAbstract;
import org.dromara.northstar.gateway.ctp.SmartGatewayConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInputOrderActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInputOrderField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInstrumentField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInvestorField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcInvestorPositionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcOrderActionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcOrderField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcQryInstrumentField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcQryInvestorField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcQryInvestorPositionField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcQryTradingAccountField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcReqAuthenticateField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcReqUserLoginField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcRspAuthenticateField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcRspInfoField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcRspUserLoginField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcSettlementInfoConfirmField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcTradeField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcTraderApi;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcTraderSpi;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcTradingAccountField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.CThostFtdcUserLogoutField;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.api.jctpv6v3v15x64apiConstants;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OptionsTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.TradeTypeEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;

public class TdSpi extends CThostFtdcTraderSpi {

	private static final Logger logger = LoggerFactory.getLogger(TdSpi.class);

	private GatewayAbstract gatewayAdapter;
	private String logInfo;
	private String gatewayId;
	private CtpGatewaySettings settings;
	private AtomicInteger contractLoaded = new AtomicInteger();

	private String investorName = "";

	private Table<PositionDirectionEnum, Contract, Set<JSONObject>> positionCacheTable = HashBasedTable.create();
	/* orderRef -> submitOrderReq */
	private Map<String, SubmitOrderReq> orderReqMap = new ConcurrentHashMap<>();
	/* originOrderId -> orderReq */
	private Map<String, String> orderRefMap = new ConcurrentHashMap<>();
	
	private ExecutorService exec;
	
	private Timer accTimer;
	private Timer posTimer;

	TdSpi(GatewayAbstract gatewayAdapter) {
		this.gatewayAdapter = gatewayAdapter;
		this.settings = (CtpGatewaySettings) gatewayAdapter.gatewayDescription().getSettings();
		this.gatewayId = gatewayAdapter.gatewayId();
		this.logInfo = "交易网关ID-[" + this.gatewayId + "] [→]";
		if(logger.isInfoEnabled()) {			
			logger.info("当前TdApi版本号：{}", CThostFtdcTraderApi.GetApiVersion());
		}
	}

	private CThostFtdcTraderApi cThostFtdcTraderApi;

	private boolean loginStatus = false; // 登陆状态
	private LocalDate tradingDay;

	private AtomicBoolean instrumentQueried = new AtomicBoolean();
	private AtomicBoolean investorNameQueried = new AtomicBoolean();

	private Random random = new Random();
	private AtomicInteger reqId = new AtomicInteger(random.nextInt(1800) % (1800 - 200 + 1) + 200); // 操作请求编号
	private AtomicInteger orderRef = new AtomicInteger(random.nextInt(1800) % (1800 - 200 + 1) + 200); // 订单编号

	private boolean loginFailed = false; // 是否已经使用错误的信息尝试登录过

	private int frontId = 0; // 前置机编号
	private int sessionId = 0; // 会话编号

	private ConcurrentLinkedQueue<Order> orderBuilderCacheList = new ConcurrentLinkedQueue<>(); // 登录起始阶段缓存Order
	private ConcurrentLinkedQueue<Trade> tradeBuilderCacheList = new ConcurrentLinkedQueue<>(); // 登录起始阶段缓存Trade
	
	private void startIntervalQuery() {
		if (accTimer != null || posTimer != null) {
			logger.error("{} 定时查询线程已存在,首先终止", logInfo);
			stopQuery();
		}
		accTimer = new Timer("CTP-Acc-Query", true);
		accTimer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				if (!loginStatus) {
					logger.warn("{} 尚未登陆,跳过查询", logInfo);
					return;
				}
				if (cThostFtdcTraderApi == null) {
					logger.error("{} 定时查询线程检测到API实例不存在,退出", logInfo);
					return;
				}
				queryAccount();
			}
			
		}, 0L, 3000L);
		
		posTimer = new Timer("CTP-Pos-Query", true);
		posTimer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				if (!loginStatus) {
					logger.warn("{} 尚未登陆,跳过查询", logInfo);
					return;
				}
				if (cThostFtdcTraderApi == null) {
					logger.error("{} 定时查询线程检测到API实例不存在,退出", logInfo);
					return;
				}
				queryPosition();
			}
			
		}, 0, 1250L);
	}

	private void stopQuery() {
		if(Objects.nonNull(accTimer)) {			
			accTimer.cancel();
			accTimer = null;
		}
		if(Objects.nonNull(posTimer)) {			
			posTimer.cancel();
			posTimer = null;
		}
	}

	public void connect() {
		if (isConnected() || gatewayAdapter.getConnectionState() == ConnectionState.CONNECTING) {
			logger.warn("{} 交易接口已经连接或正在连接，不再重复连接", logInfo);
			return;
		}

		if (gatewayAdapter.getConnectionState() == ConnectionState.CONNECTED) {
			reqAuth();
			return;
		}
		exec = CommonUtils.newThreadPerTaskExecutor(getClass());
		gatewayAdapter.setConnectionState(ConnectionState.CONNECTING);
		loginStatus = false;
		instrumentQueried.set(false);
		investorNameQueried.set(false);
		contractLoaded.set(0);

		if (cThostFtdcTraderApi != null) {
			try {
				CThostFtdcTraderApi cThostFtdcTraderApiForRelease = cThostFtdcTraderApi;
				cThostFtdcTraderApi = null;
				exec.execute(() -> {
					cThostFtdcTraderApiForRelease.RegisterSpi(null);
					try {
						logger.warn("交易接口异步释放启动！");
						cThostFtdcTraderApiForRelease.Release();
						logger.warn("交易接口异步释放完成！");
					} catch (Throwable t) {
						logger.error("交易接口异步释放发生异常！", t);
					}
				});

				Thread.sleep(100);
			} catch (Throwable t) {
				logger.warn("{} 交易接口连接前释放异常", logInfo, t);
			}

		}

		logger.warn("{} 交易接口实例初始化", logInfo);
		String envTmpDir = System.getProperty("java.io.tmpdir");
		String tempFilePath = envTmpDir + File.separator + "xyz" + File.separator + "redtorch" + File.separator + "gateway" + File.separator + "ctp" + File.separator + "jctpv6v3v15x64api"
				+ File.separator + "CTP_FLOW_TEMP" + File.separator + "TD_" + gatewayId;
		File tempFile = new File(tempFilePath);
		if (!tempFile.getParentFile().exists()) {
			try {
				FileUtils.forceMkdirParent(tempFile);
				logger.info("{} 交易接口创建临时文件夹 {}", logInfo, tempFile.getParentFile().getAbsolutePath());
			} catch (IOException e) {
				logger.error("{} 交易接口创建临时文件夹失败{}", logInfo, tempFile.getParentFile().getAbsolutePath(), e);
			}
		}

		logger.warn("{} 交易接口使用临时文件夹{}", logInfo, tempFile.getParentFile().getAbsolutePath());

		exec.execute(() -> {
			try {
				SmartGatewayConnector smartConnector = new SmartGatewayConnector(settings.getBroker().getHosts());
				String tdHost = smartConnector.bestEndpoint();
				int tdPort = settings.getBroker().getTdPort();
				logger.info("{} 使用IP [{}:{}] 连接交易网关", logInfo, tdHost, tdPort);
				cThostFtdcTraderApi = CThostFtdcTraderApi.CreateFtdcTraderApi(tempFile.getAbsolutePath());
				cThostFtdcTraderApi.RegisterSpi(this);
				cThostFtdcTraderApi.RegisterFront("tcp://" + tdHost + ":" + tdPort);
				cThostFtdcTraderApi.Init();
			} catch (Throwable t) {
				logger.error("{} 交易接口连接异常", logInfo, t);
			}
		});

		exec.execute(() -> {
			try {
				Thread.sleep(30000);
				if (!(isConnected() && investorNameQueried.get() && instrumentQueried.get())) {
					logger.error("{} 交易接口连接超时,尝试断开", logInfo);
					gatewayAdapter.disconnect();
				}
			} catch (Throwable t) {
				logger.error("{} 交易接口处理连接超时线程异常", logInfo, t);
			}
		});
	}

	public void disconnect() {
		try {
			this.stopQuery();
			if (cThostFtdcTraderApi != null && gatewayAdapter.getConnectionState() != ConnectionState.DISCONNECTING) {
				logger.warn("{} 交易接口实例开始关闭并释放", logInfo);
				loginStatus = false;
				instrumentQueried.set(false);
				investorNameQueried.set(false);
				gatewayAdapter.setConnectionState(ConnectionState.DISCONNECTING);
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.LOGGING_OUT, gatewayId);
				
				CThostFtdcTraderApi cThostFtdcTraderApiForRelease = cThostFtdcTraderApi;
				cThostFtdcTraderApi = null;
				exec.execute(() -> {
					cThostFtdcTraderApiForRelease.RegisterSpi(null);
					try {
						logger.warn("交易接口异步释放启动！");
						cThostFtdcTraderApiForRelease.Release();
						logger.warn("交易接口异步释放完成！");
					} catch (Throwable t) {
						logger.error("交易接口异步释放发生异常！", t);
					}
				});
				gatewayAdapter.setConnectionState(ConnectionState.DISCONNECTED);
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.LOGGED_OUT, gatewayId);
				exec.close();
				logger.warn("{} 交易接口实例关闭并异步释放", logInfo);
			} else {
				logger.warn("{} 交易接口实例不存在或正在关闭释放,无需操作", logInfo);
			}
		} catch (Throwable t) {
			logger.error("{} 交易接口实例关闭并释放异常", logInfo, t);
		}

	}

	public boolean isConnected() {
		return gatewayAdapter.getConnectionState() == ConnectionState.CONNECTED && loginStatus;
	}

	public void queryAccount() {
		if (cThostFtdcTraderApi == null) {
			logger.warn("{} 交易接口尚未初始化,无法查询账户", logInfo);
			return;
		}
		if (!loginStatus) {
			logger.warn("{} 交易接口尚未登录,无法查询账户", logInfo);
			return;
		}
		if (!instrumentQueried.get()) {
			logger.warn("{} 交易接口尚未获取到合约信息,无法查询账户", logInfo);
			return;
		}
		if (!investorNameQueried.get()) {
			logger.warn("{} 交易接口尚未获取到投资者姓名,无法查询账户", logInfo);
			return;
		}
		try {
			CThostFtdcQryTradingAccountField cThostFtdcQryTradingAccountField = new CThostFtdcQryTradingAccountField();
			cThostFtdcTraderApi.ReqQryTradingAccount(cThostFtdcQryTradingAccountField, reqId.incrementAndGet());
		} catch (Throwable t) {
			logger.error("{} 交易接口查询账户异常", logInfo, t);
		}

	}

	public void queryPosition() {
		if (cThostFtdcTraderApi == null) {
			logger.warn("{} 交易接口尚未初始化,无法查询持仓", logInfo);
			return;
		}
		if (!loginStatus) {
			logger.warn("{} 交易接口尚未登录,无法查询持仓", logInfo);
			return;
		}

		if (!instrumentQueried.get()) {
			logger.warn("{} 交易接口尚未获取到合约信息,无法查询持仓", logInfo);
			return;
		}
		if (!investorNameQueried.get()) {
			logger.warn("{} 交易接口尚未获取到投资者姓名,无法查询持仓", logInfo);
			return;
		}

		try {
			CThostFtdcQryInvestorPositionField cThostFtdcQryInvestorPositionField = new CThostFtdcQryInvestorPositionField();
			cThostFtdcQryInvestorPositionField.setBrokerID(settings.getBroker().getBrokerId());
			cThostFtdcQryInvestorPositionField.setInvestorID(settings.getUserId());
			cThostFtdcTraderApi.ReqQryInvestorPosition(cThostFtdcQryInvestorPositionField, reqId.incrementAndGet());
		} catch (Throwable t) {
			logger.error("{} 交易接口查询持仓异常", logInfo, t);
		}

	}

	public String submitOrder(SubmitOrderReq submitOrderReq) {
		if (cThostFtdcTraderApi == null) {
			logger.warn("{} 交易接口尚未初始化,无法发单", logInfo);
			return null;
		}

		if (!loginStatus) {
			logger.warn("{} 交易接口尚未登录,无法发单", logInfo);
			return null;
		}
		
		String orderRefStr = orderRef.incrementAndGet() + "";
		orderReqMap.put(orderRefStr, submitOrderReq);
		orderRefMap.put(submitOrderReq.originOrderId(), orderRefStr);
		
		CThostFtdcInputOrderField cThostFtdcInputOrderField = new CThostFtdcInputOrderField();
		cThostFtdcInputOrderField.setInstrumentID(submitOrderReq.contract().symbol());
		cThostFtdcInputOrderField.setLimitPrice(submitOrderReq.price());
		cThostFtdcInputOrderField.setVolumeTotalOriginal(submitOrderReq.volume());
		cThostFtdcInputOrderField.setOrderPriceType(CtpConstant.orderPriceTypeMap.getOrDefault(submitOrderReq.orderPriceType(), Character.valueOf('\0')));
		cThostFtdcInputOrderField.setDirection(CtpConstant.directionMap.getOrDefault(submitOrderReq.direction(), Character.valueOf('\0')));
		cThostFtdcInputOrderField.setCombOffsetFlag(String.valueOf(CtpConstant.offsetFlagMap.getOrDefault(submitOrderReq.offsetFlag(), Character.valueOf('\0'))));
		cThostFtdcInputOrderField.setInvestorID(settings.getUserId());
		cThostFtdcInputOrderField.setUserID(settings.getUserId());
		cThostFtdcInputOrderField.setBrokerID(settings.getBroker().getBrokerId());
		cThostFtdcInputOrderField.setExchangeID(CtpConstant.exchangeMap.getOrDefault(submitOrderReq.contract().exchange(), ""));
		cThostFtdcInputOrderField.setCombHedgeFlag(CtpConstant.hedgeFlagMap.get(HedgeFlagEnum.HF_Speculation));
		cThostFtdcInputOrderField.setContingentCondition(CtpConstant.contingentConditionMap.get(submitOrderReq.contingentCondition()));
		cThostFtdcInputOrderField.setForceCloseReason(CtpConstant.forceCloseReasonMap.get(ForceCloseReasonEnum.FCR_NotForceClose));
		cThostFtdcInputOrderField.setIsAutoSuspend(0);
		cThostFtdcInputOrderField.setIsSwapOrder(0);
		cThostFtdcInputOrderField.setMinVolume(submitOrderReq.minVolume());
		cThostFtdcInputOrderField.setTimeCondition(CtpConstant.timeConditionMap.getOrDefault(submitOrderReq.timeCondition(), Character.valueOf('\0')));
		cThostFtdcInputOrderField.setVolumeCondition(CtpConstant.volumeConditionMap.getOrDefault(submitOrderReq.volumeCondition(), Character.valueOf('\0')));
		cThostFtdcInputOrderField.setStopPrice(submitOrderReq.stopPrice());
		cThostFtdcInputOrderField.setOrderRef(orderRefStr);

		logger.info("{} 委托单详情：{} {}", logInfo, submitOrderReq.contract().symbol(), submitOrderReq);
		if(logger.isDebugEnabled()) {			
			logger.debug("{} 原始委托单：{}", logInfo, JSON.toJSONString(cThostFtdcInputOrderField));
		}
		cThostFtdcTraderApi.ReqOrderInsert(cThostFtdcInputOrderField, reqId.incrementAndGet());

		return submitOrderReq.originOrderId();
	}

	// 撤单
	public boolean cancelOrder(String originOrderId) {

		if (cThostFtdcTraderApi == null) {
			logger.warn("{} 交易接口尚未初始化，无法撤单", logInfo);
			return false;
		}

		if (!loginStatus) {
			logger.warn("{} 交易接口尚未登录，无法撤单", logInfo);
			return false;
		}

		if (StringUtils.isBlank(originOrderId)) {
			logger.error("{} 订单ID参数为空，无法撤单", logInfo);
			return false;
		}

		if (!orderRefMap.containsKey(originOrderId)) {
			logger.error("{} 交易接口未能找到有效定单号，无法撤单", logInfo);
			return false;
		}

		try {
			SubmitOrderReq orderReq = orderReqMap.get(orderRefMap.get(originOrderId));
			
			CThostFtdcInputOrderActionField cThostFtdcInputOrderActionField = new CThostFtdcInputOrderActionField();
			cThostFtdcInputOrderActionField.setInstrumentID(orderReq.contract().symbol());
			cThostFtdcInputOrderActionField.setExchangeID(orderReq.contract().exchange().toString());
			cThostFtdcInputOrderActionField.setOrderRef(orderRefMap.get(originOrderId));
			cThostFtdcInputOrderActionField.setFrontID(frontId);
			cThostFtdcInputOrderActionField.setSessionID(sessionId);
			
			cThostFtdcInputOrderActionField.setActionFlag(jctpv6v3v15x64apiConstants.THOST_FTDC_AF_Delete);
			cThostFtdcInputOrderActionField.setBrokerID(settings.getBroker().getBrokerId());
			cThostFtdcInputOrderActionField.setInvestorID(settings.getUserId());
			cThostFtdcInputOrderActionField.setUserID(settings.getUserId());
			cThostFtdcTraderApi.ReqOrderAction(cThostFtdcInputOrderActionField, reqId.incrementAndGet());
			return true;
		} catch (Throwable t) {
			logger.error("{} 撤单异常", logInfo, t);
			return false;
		}

	}

	private void reqAuth() {
		if (loginFailed) {
			logger.warn("{} 交易接口登录曾发生错误,不再登录,以防被锁", logInfo);
			return;
		}

		if (cThostFtdcTraderApi == null) {
			logger.warn("{} 发起客户端验证请求错误,交易接口实例不存在", logInfo);
			return;
		}

		if (StringUtils.isEmpty(settings.getBroker().getBrokerId())) {
			logger.error("{} BrokerID不允许为空", logInfo);
			return;
		}

		if (StringUtils.isEmpty(settings.getUserId())) {
			logger.error("{} UserId不允许为空", logInfo);
			return;
		}

		if (StringUtils.isEmpty(settings.getPassword())) {
			logger.error("{} Password不允许为空", logInfo);
			return;
		}

		if (StringUtils.isEmpty(settings.getBroker().getAppId())) {
			logger.error("{} AppId不允许为空", logInfo);
			return;
		}
		if (StringUtils.isEmpty(settings.getBroker().getAuthCode())) {
			logger.error("{} AuthCode不允许为空", logInfo);
			return;
		}

		try {
			gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.LOGGING_IN, gatewayId);
			CThostFtdcReqAuthenticateField authenticateField = new CThostFtdcReqAuthenticateField();
			authenticateField.setAppID(settings.getBroker().getAppId());
			authenticateField.setAuthCode(settings.getBroker().getAuthCode());
			authenticateField.setBrokerID(settings.getBroker().getBrokerId());
			authenticateField.setUserProductInfo(settings.getBroker().getAppId());
			authenticateField.setUserID(settings.getUserId());
			logger.debug("CTP 认证信息： APPID:{}, AUTH_CODE:{}, BROKER_ID:{}, PRODUCT_INFO: {}, USER_ID:{}",
					settings.getBroker().getAppId(), settings.getBroker().getAuthCode(), settings.getBroker().getBrokerId(), settings.getBroker().getAppId(), settings.getUserId());
			cThostFtdcTraderApi.ReqAuthenticate(authenticateField, reqId.incrementAndGet());
		} catch (Throwable t) {
			logger.error("{} 发起客户端验证异常", logInfo, t);
			gatewayAdapter.disconnect();
		}

	}

	// 前置机联机回报
	@Override
	public void OnFrontConnected() {
		try {
			logger.info("{} 交易接口前置机已连接", logInfo);
			// 修改前置机连接状态
			gatewayAdapter.setConnectionState(ConnectionState.CONNECTED);

			reqAuth();

		} catch (Throwable t) {
			logger.error("{} OnFrontConnected Exception", logInfo, t);
		}
	}

	// 前置机断开回报
	@Override
	public void OnFrontDisconnected(int nReason) {
		try {
			logger.warn("{} 交易接口前置机已断开, 原因:{}", logInfo, nReason);
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
				logger.info("{} 交易接口登录成功 TradingDay:{},SessionID:{},BrokerID:{},UserID:{}", logInfo, pRspUserLogin.getTradingDay(), pRspUserLogin.getSessionID(), pRspUserLogin.getBrokerID(),
						pRspUserLogin.getUserID());
				sessionId = pRspUserLogin.getSessionID();
				frontId = pRspUserLogin.getFrontID();
				// 修改登录状态为true
				loginStatus = true;
				tradingDay = LocalDate.parse(pRspUserLogin.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
				logger.info("{} 交易接口获取到的交易日为{}", logInfo, tradingDay);

				// 确认结算单
				CThostFtdcSettlementInfoConfirmField settlementInfoConfirmField = new CThostFtdcSettlementInfoConfirmField();
				settlementInfoConfirmField.setBrokerID(settings.getBroker().getBrokerId());
				settlementInfoConfirmField.setInvestorID(settings.getUserId());
				cThostFtdcTraderApi.ReqSettlementInfoConfirm(settlementInfoConfirmField, reqId.incrementAndGet());

				// 不合法的登录
				if (pRspInfo.getErrorID() == 3) {
					gatewayAdapter.setAuthErrorFlag(true);
					return;
				}

				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.LOGGED_IN, gatewayId);
			} else {
				logger.error("{} 交易接口登录回报错误 错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
				loginFailed = true;
			}
		} catch (Throwable t) {
			logger.error("{} 交易接口处理登录回报异常", logInfo, t);
			loginFailed = true;
		}

	}

	// 心跳警告
	@Override
	public void OnHeartBeatWarning(int nTimeLapse) {
		logger.warn("{} 交易接口心跳警告, Time Lapse:{}", logInfo, nTimeLapse);
	}

	// 登出回报
	@Override
	public void OnRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			if (pRspInfo.getErrorID() != 0) {
				logger.error("{} OnRspUserLogout!错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
			} else {
				logger.info("{} OnRspUserLogout!BrokerID:{},UserId:{}", logInfo, pUserLogout.getBrokerID(), pUserLogout.getUserID());

			}
		} catch (Throwable t) {
			logger.error("{} 交易接口处理登出回报错误", logInfo, t);
		}

		loginStatus = false;
	}

	// 错误回报
	@Override
	public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			logger.error("{} 交易接口错误回报!错误ID:{},错误信息:{},请求ID:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg(), nRequestID);
			if (instrumentQueried.get()) {
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.NOTICE, Notice.builder()
						.content(logInfo + "交易接口错误回报:" + pRspInfo.getErrorMsg() + "，错误ID:" + pRspInfo.getErrorID())
						.status(CommonStatusEnum.COMS_ERROR)
						.build());
			}
			// CTP查询尚未就绪,断开
			if (pRspInfo.getErrorID() == 90) {
				gatewayAdapter.disconnect();
			}
		} catch (Throwable t) {
			logger.error("{} OnRspError Exception", logInfo, t);
		}
	}

	// 验证客户端回报
	@Override
	public void OnRspAuthenticate(CThostFtdcRspAuthenticateField pRspAuthenticateField, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			if (pRspInfo != null) {
				if (pRspInfo.getErrorID() == 0) {
					logger.info("{} {}", logInfo, "交易接口客户端验证成功");
					CThostFtdcReqUserLoginField reqUserLoginField = new CThostFtdcReqUserLoginField();
					reqUserLoginField.setBrokerID(settings.getBroker().getBrokerId());
					reqUserLoginField.setUserID(settings.getUserId());
					reqUserLoginField.setPassword(settings.getPassword());
					logger.debug("CTP 登陆信息： BROKER_ID:{}, USER:{}, PWD:{}",
							settings.getBroker().getAppId(), settings.getUserId(), settings.getPassword());
					cThostFtdcTraderApi.ReqUserLogin(reqUserLoginField, reqId.incrementAndGet());

				} else {

					logger.error("{} 交易接口客户端验证失败 错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
					loginFailed = true;

					// 客户端验证失败
					if (pRspInfo.getErrorID() == 63) {
						gatewayAdapter.setAuthErrorFlag(true);
					}
				}
			} else {
				loginFailed = true;
				logger.error("{} 处理交易接口客户端验证回报错误,回报信息为空", logInfo);
			}
		} catch (Throwable t) {
			loginFailed = true;
			logger.error("{} 处理交易接口客户端验证回报异常", logInfo, t);
		}
	}

	// 撤单错误回报
	@Override
	public void OnRspOrderAction(CThostFtdcInputOrderActionField pInputOrderAction, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		if (pRspInfo != null) {
			logger.error("{} 交易接口撤单错误回报(OnRspOrderAction) 错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
			if (instrumentQueried.get()) {
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.NOTICE, Notice.builder()
						.content(logInfo + "交易接口撤单错误回报，错误ID:" + pRspInfo.getErrorID() + "，错误信息:" + pRspInfo.getErrorMsg())
						.status(CommonStatusEnum.COMS_ERROR)
						.build());
			}
		} else {
			logger.error("{} 处理交易接口撤单错误回报(OnRspOrderAction)错误,无有效信息", logInfo);
		}
	}

	// 确认结算信息回报
	@Override
	public void OnRspSettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField pSettlementInfoConfirm, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			if(pRspInfo == null) {
				logger.warn("交易结算信息为空");
			} else if (pRspInfo.getErrorID() == 0) {
				logger.info("{} 交易接口结算信息确认完成", logInfo);
			} else {
				logger.error("{} 交易接口结算信息确认出错 错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
				gatewayAdapter.disconnect();
				return;
			}

			// 防止被限流
			Thread.sleep(1000);

			logger.info("{} 交易接口开始查询投资者信息", logInfo);
			CThostFtdcQryInvestorField pQryInvestor = new CThostFtdcQryInvestorField();
			pQryInvestor.setInvestorID(settings.getUserId());
			pQryInvestor.setBrokerID(settings.getBroker().getBrokerId());
			cThostFtdcTraderApi.ReqQryInvestor(pQryInvestor, reqId.addAndGet(1));
		} catch (Throwable t) {
			logger.error("{} 处理结算单确认回报错误", logInfo, t);
			gatewayAdapter.disconnect();
		}
	}

	// 持仓查询回报
	@Override
	public void OnRspQryInvestorPosition(CThostFtdcInvestorPositionField pInvestorPosition, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			if (pInvestorPosition == null || StringUtils.isEmpty(pInvestorPosition.getInstrumentID())) {
				return;
			}
			String symbol = pInvestorPosition.getInstrumentID();

			if (!instrumentQueried.get()) {
				logger.debug("{} 尚未获取到合约信息,暂时不处理持仓数据,代码{}", logInfo, symbol);
				return;
			}

			Contract contract = gatewayAdapter.mktCenter.getContract(ChannelType.CTP, symbol).contract();
			PositionDirectionEnum direction = CtpConstant.posiDirectionMapReverse.getOrDefault(pInvestorPosition.getPosiDirection(), PositionDirectionEnum.PD_Unknown);
			synchronized (positionCacheTable) {
				if(!positionCacheTable.contains(direction, contract)) {
					positionCacheTable.put(direction, contract, new HashSet<>());
				}
				positionCacheTable.get(direction, contract).add((JSONObject) JSON.toJSON(pInvestorPosition));
			}

			// 回报结束
			if (bIsLast) {
				positionCacheTable.row(PositionDirectionEnum.PD_Long).entrySet().forEach(e -> 
					gatewayAdapter.getEventEngine()
						.emitEvent(NorthstarEventType.POSITION, convertPosition(PositionDirectionEnum.PD_Long, e.getKey(), e.getValue())));
				positionCacheTable.row(PositionDirectionEnum.PD_Short).entrySet().forEach(e -> 
					gatewayAdapter.getEventEngine()
						.emitEvent(NorthstarEventType.POSITION, convertPosition(PositionDirectionEnum.PD_Short, e.getKey(), e.getValue())));
				// 清空缓存
				positionCacheTable.clear();
			}

		} catch (Throwable t) {
			logger.error("{} 处理查询持仓回报异常", logInfo, t);
			gatewayAdapter.disconnect();
		}
	}

	private static final String TDPOS = "1";
	
	private Position convertPosition(PositionDirectionEnum dir, Contract contract, Set<JSONObject> jsons) {
		int tdPosition = 0;
		int tdFrozen = 0;
		int ydPosition = 0;
		int ydFrozen = 0;
		int position = 0;
		
		double openCost = 0;
		double positionProfit = 0;
		double useMargin = 0;
		double exchangeMargin = 0;
		double contractValue = 0;
		
		for(JSONObject json : jsons) {
			if(json.getString("positionDate").equals(TDPOS)) {
				tdPosition += json.getIntValue("position");
				tdFrozen += Math.max(json.getIntValue("shortFrozen"), json.getIntValue("longFrozen"));
			} else {
				ydPosition += json.getIntValue("position");
				ydFrozen += Math.max(json.getIntValue("shortFrozen"), json.getIntValue("longFrozen"));
			}
			position += json.getIntValue("position");
			openCost += json.getDoubleValue("openCost");
			positionProfit += json.getDoubleValue("positionProfit");
			useMargin += json.getDoubleValue("useMargin");
			exchangeMargin += json.getDoubleValue("exchangeMargin");
			contractValue += json.getDoubleValue("positionCost");
		}
		
		double openPrice = position == 0 ? 0 : openCost / (position * contract.multiplier());
		double openPriceDiff = position == 0 ? 0 : positionProfit / (position * contract.multiplier());
		
		return Position.builder()
				.positionId(String.format("%s@%s@%s", contract.unifiedSymbol(), dir, gatewayId))
				.gatewayId(gatewayId)
				.positionDirection(dir)
				.position(position)
				.frozen(tdFrozen + ydFrozen)
				.tdPosition(tdPosition)
				.tdFrozen(tdFrozen)
				.ydPosition(ydPosition)
				.ydFrozen(ydFrozen)
				.openPrice(openPrice)
				.openPriceDiff(openPriceDiff)
				.positionProfit(positionProfit)
				.positionProfitRatio(useMargin == 0 ? 0 : positionProfit / useMargin)
				.contract(contract)
				.contractValue(contractValue)
				.useMargin(useMargin)
				.exchangeMargin(exchangeMargin)
				.updateTimestamp(System.currentTimeMillis())
				.build();
	}

	// 账户查询回报
	@Override
	public void OnRspQryTradingAccount(CThostFtdcTradingAccountField pTradingAccount, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.ACCOUNT, Account.builder()
					.currency(CurrencyEnum.CNY)
					.available(pTradingAccount.getAvailable())
					.closeProfit(pTradingAccount.getCloseProfit())
					.commission(pTradingAccount.getCommission())
					.gatewayId(gatewayId)
					.margin(pTradingAccount.getCurrMargin())
					.positionProfit(pTradingAccount.getPositionProfit())
					.preBalance(pTradingAccount.getPreBalance())
					.deposit(pTradingAccount.getDeposit())
					.withdraw(pTradingAccount.getWithdraw())
					.balance(pTradingAccount.getBalance())
					.updateTimestamp(System.currentTimeMillis())
					.build());
		} catch (Throwable t) {
			logger.error("{} 处理查询账户回报异常", logInfo, t);
			gatewayAdapter.disconnect();
		}
	}

	@Override
	public void OnRspQryInvestor(CThostFtdcInvestorField pInvestor, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		try {
			if (pRspInfo != null && pRspInfo.getErrorID() != 0) {
				logger.error("{} 查询投资者信息失败 错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
				gatewayAdapter.disconnect();
			} else {
				if (pInvestor != null) {
					investorName = pInvestor.getInvestorName();
					logger.info("{} 交易接口获取到的投资者名为:{}", logInfo, investorName);
				} else {
					logger.error("{} 交易接口未能获取到投资者名", logInfo);
				}
			}

			if (bIsLast) {
				if (StringUtils.isBlank(investorName)) {
					logger.warn("{} 交易接口未能获取到投资者名", logInfo);
					gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.NOTICE, Notice.builder()
							.content(logInfo + "交易接口投资者名为空")
							.status(CommonStatusEnum.COMS_WARN)
							.build());
				}
				investorNameQueried.set(true);
				// 防止被限流
				Thread.sleep(1000);
				// 查询所有合约
				logger.info("{} 交易接口开始查询合约信息", logInfo);
				CThostFtdcQryInstrumentField cThostFtdcQryInstrumentField = new CThostFtdcQryInstrumentField();
				cThostFtdcTraderApi.ReqQryInstrument(cThostFtdcQryInstrumentField, reqId.incrementAndGet());
			}
		} catch (Throwable t) {
			logger.error("{} 处理查询投资者回报异常", logInfo, t);
			gatewayAdapter.disconnect();
		}
	}
	
	// 合约查询回报
	@Override
	public void OnRspQryInstrument(CThostFtdcInstrumentField pInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
		if(logger.isTraceEnabled()) {
			logger.trace("合约信息原始信息：{}", JSON.toJSONString(pInstrument));
		}
		try {
			String symbol = pInstrument.getInstrumentID();
			ExchangeEnum exchange = CtpConstant.exchangeMapReverse.getOrDefault(pInstrument.getExchangeID(), ExchangeEnum.UnknownExchange);
			ProductClassEnum productClass = CtpConstant.productTypeMapReverse.getOrDefault(pInstrument.getProductClass(), ProductClassEnum.UnknownProductClass);
			String unifiedSymbol = String.format("%s@%s@%s", symbol, exchange, productClass);
			String contractId = String.format("%s@%s", unifiedSymbol, ChannelType.CTP);
			LocalDate dueDate = LocalDate.MAX;
			if(StringUtils.isNotBlank(pInstrument.getExpireDate())) {
				dueDate = LocalDate.parse(pInstrument.getExpireDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
			}
			Optional<ContractDefinition> cd = gatewayAdapter.mktCenter.getDefinition(exchange, productClass, contractId);
			String name = cd.isPresent() ? symbol.replaceAll("^[A-z]+", cd.get().name()) : pInstrument.getInstrumentName();
			CtpContract contract = CtpContract.builder()
					.gatewayId(ChannelType.CTP.toString())
					.channelType(ChannelType.CTP)
					.symbol(symbol)
					.name(name)
					.fullName(name)
					.thirdPartyId(symbol + "@CTP")
					.exchange(exchange)
					.productClass(productClass)
					.unifiedSymbol(unifiedSymbol)
					.contractId(contractId)
					.multiplier(Math.max(1, pInstrument.getVolumeMultiple()))
					.priceTick(pInstrument.getPriceTick())
					.currency(CurrencyEnum.CNY)
					.lastTradeDate(dueDate)
					.strikePrice(pInstrument.getStrikePrice())
					.optionsType(CtpConstant.optionTypeMapReverse.getOrDefault(pInstrument.getOptionsType(), OptionsTypeEnum.O_Unknown))
					.underlyingSymbol(Optional.ofNullable(pInstrument.getUnderlyingInstrID()).orElse(""))
					.underlyingMultiplier(pInstrument.getUnderlyingMultiple())
					.maxLimitOrderVolume(pInstrument.getMaxLimitOrderVolume())
					.minLimitOrderVolume(pInstrument.getMinLimitOrderVolume())
					.maxMarketOrderVolume(pInstrument.getMaxMarketOrderVolume())
					.minMarketOrderVolume(pInstrument.getMinMarketOrderVolume())
					.maxMarginSideAlgorithm(pInstrument.getMaxMarginSideAlgorithm() == '1')
					.longMarginRatio(pInstrument.getLongMarginRatio())
					.shortMarginRatio(pInstrument.getShortMarginRatio())
					.build();

			gatewayAdapter.mktCenter.addInstrument(contract);
			contractLoaded.incrementAndGet();

			if (bIsLast) {
				logger.info("{} 交易接口合约信息获取完成！共计{}条", logInfo, contractLoaded.get());

				instrumentQueried.set(true);
				this.startIntervalQuery();

				logger.info("{} 交易接口开始推送缓存Order，共计{}条", logInfo, orderBuilderCacheList.size());
				for (Order order : orderBuilderCacheList) {
					try {
						Contract c = gatewayAdapter.mktCenter.getContract(ChannelType.CTP, order.contract().symbol()).contract();
						order = order.toBuilder().contract(c).build();
						gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.ORDER, order);
					} catch(NoSuchElementException e) {
						logger.error("{} 未能正确获取到合约信息，代码{}", logInfo, order.contract().symbol());
						logger.error("", e);
					}
				}
				orderBuilderCacheList.clear();

				logger.info("{} 交易接口开始推送缓存Trade，共计{}条", logInfo, tradeBuilderCacheList.size());
				for (Trade trade : tradeBuilderCacheList) {
					try {
						Contract c = gatewayAdapter.mktCenter.getContract(ChannelType.CTP, trade.contract().symbol()).contract();
						gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.TRADE, trade.toBuilder().contract(c).build());
					} catch(NoSuchElementException e) {
						logger.error("{} 未能正确获取到合约信息，代码{}", logInfo, trade.contract().symbol());
						logger.error("", e);
					}
				}
				tradeBuilderCacheList.clear();
				gatewayAdapter.mktCenter.loadContractGroup(ChannelType.CTP);
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.GATEWAY_READY, gatewayId);
			}
		} catch (Throwable t) {
			logger.error("{}OnRspQryInstrument Exception", logInfo, t);
		}

	}

	// 定单回报
	@Override
	public void OnRtnOrder(CThostFtdcOrderField pOrder) {
		if(logger.isTraceEnabled()) {
			logger.trace("订单回报原始信息：{}", JSON.toJSONString(pOrder));
		}
		try {
			String symbol = pOrder.getInstrumentID();
			String orderRefStr = StringUtils.trim(pOrder.getOrderRef());
			SubmitOrderReq orderReq = orderReqMap.get(orderRefStr);
			String originOrderId = Objects.nonNull(orderReq) ? orderReq.originOrderId() : "";
			DirectionEnum direction = CtpConstant.directionMapReverse.getOrDefault(pOrder.getDirection(), DirectionEnum.D_Unknown);
			OffsetFlagEnum offsetFlag = CtpConstant.offsetMapReverse.getOrDefault(pOrder.getCombOffsetFlag().toCharArray()[0], OffsetFlagEnum.OF_Unknown);

			double price = pOrder.getLimitPrice();

			int totalVolume = pOrder.getVolumeTotalOriginal();
			int tradedVolume = pOrder.getVolumeTraded();

			OrderStatusEnum orderStatus = CtpConstant.statusMapReverse.get(pOrder.getOrderStatus());
			String statusMsg = pOrder.getStatusMsg();

			LocalDate orderDate = StringUtils.isBlank(pOrder.getInsertDate()) || Integer.parseInt(pOrder.getInsertDate()) == 0
										? LocalDate.now()
										: LocalDate.parse(pOrder.getInsertDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
			LocalTime orderTime = StringUtils.isBlank(pOrder.getInsertTime())
										? LocalTime.now() 
										: LocalTime.parse(pOrder.getInsertTime(), DateTimeConstant.T_FORMAT_FORMATTER);

			HedgeFlagEnum hedgeFlag = CtpConstant.hedgeFlagMapReverse.getOrDefault(pOrder.getCombHedgeFlag(), HedgeFlagEnum.HF_Unknown);
			ContingentConditionEnum contingentCondition = CtpConstant.contingentConditionMapReverse.getOrDefault(pOrder.getContingentCondition(), ContingentConditionEnum.CC_Unknown);
			ForceCloseReasonEnum forceCloseReason = CtpConstant.forceCloseReasonMapReverse.getOrDefault(pOrder.getForceCloseReason(), ForceCloseReasonEnum.FCR_Unknown);
			TimeConditionEnum timeCondition = CtpConstant.timeConditionMapReverse.getOrDefault(pOrder.getTimeCondition(), TimeConditionEnum.TC_Unknown);

			String gtdDate = pOrder.getGTDDate();

			VolumeConditionEnum volumeCondition = CtpConstant.volumeConditionMapReverse.getOrDefault(pOrder.getVolumeCondition(), VolumeConditionEnum.VC_Unknown);
			OrderPriceTypeEnum orderPriceType = CtpConstant.orderPriceTypeMapReverse.getOrDefault(pOrder.getOrderPriceType(), OrderPriceTypeEnum.OPT_Unknown);

			int minVolume = pOrder.getMinVolume();
			double stopPrice = pOrder.getStopPrice();

			Contract contractPlaceholder = Contract.builder().symbol(symbol).build();
			Order order = Order.builder()
					.originOrderId(originOrderId)
					.orderId(originOrderId)
					.tradingDay(tradingDay)
					.contract(contractPlaceholder)
					.direction(direction)
					.offsetFlag(offsetFlag)
					.orderDate(orderDate)
					.orderTime(orderTime)
					.updateDate(orderDate)
					.updateTime(orderTime)
					.orderStatus(orderStatus)
					.price(price)
					.totalVolume(totalVolume)
					.tradedVolume(tradedVolume)
					.statusMsg(statusMsg)
					.gatewayId(gatewayId)
					.hedgeFlag(hedgeFlag)
					.contingentCondition(contingentCondition)
					.forceCloseReason(forceCloseReason)
					.timeCondition(timeCondition)
					.gtdDate(gtdDate)
					.volumeCondition(volumeCondition)
					.minVolume(minVolume)
					.stopPrice(stopPrice)
					.orderPriceType(orderPriceType)
					.build();

			if (instrumentQueried.get()) {
				Contract c = gatewayAdapter.mktCenter.getContract(ChannelType.CTP, symbol).contract();
				order = order.toBuilder().contract(c).build();
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.ORDER, order);
			} else {
				orderBuilderCacheList.add(order);
			}
			logger.info("{} 委托回报：合约{}，单号{}，方向{}，开平{}，价格{}，止损{}，手数{}，交易日{}，类型{} & {}，状态{}", logInfo,
					symbol, originOrderId, direction, offsetFlag, price, stopPrice, tradedVolume, tradingDay, hedgeFlag, timeCondition, orderStatus);
		} catch (Throwable t) {
			logger.error("{} OnRtnOrder Exception：{}", logInfo, JSON.toJSONString(pOrder), t);
		}
	}

	// 成交回报
	@Override
	public void OnRtnTrade(CThostFtdcTradeField pTrade) {
		if(logger.isTraceEnabled()) {
			logger.trace("成交回报原始信息：{}", JSON.toJSONString(pTrade));
		}
		try {
			String orderRefStr = StringUtils.trim(pTrade.getOrderRef());
			SubmitOrderReq orderReq = orderReqMap.get(orderRefStr);
			String originOrderId = Objects.nonNull(orderReq) ? orderReq.originOrderId() : "";
			String symbol = pTrade.getInstrumentID();
			DirectionEnum direction = CtpConstant.directionMapReverse.getOrDefault(pTrade.getDirection(), DirectionEnum.D_Unknown);
			OffsetFlagEnum offsetFlag = CtpConstant.offsetMapReverse.getOrDefault(pTrade.getOffsetFlag(), OffsetFlagEnum.OF_Unknown);
			double price = pTrade.getPrice();
			int volume = pTrade.getVolume();
			LocalDate tradeDate = StringUtils.isBlank(pTrade.getTradeDate()) || Integer.parseInt(pTrade.getTradeDate()) == 0
										? LocalDate.now() 
										: LocalDate.parse(pTrade.getTradeDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
			LocalTime tradeTime = StringUtils.isBlank(pTrade.getTradeTime()) 
										? LocalTime.now() 
										: LocalTime.parse(pTrade.getTradeTime(), DateTimeConstant.T_FORMAT_FORMATTER);
			LocalDateTime tradeDatetime = LocalDateTime.of(tradeDate, tradeTime);
			long tradeTimestamp = tradeDatetime.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond() * 1000;

			HedgeFlagEnum hedgeFlag = CtpConstant.hedgeFlagMapReverse.getOrDefault(String.valueOf(pTrade.getHedgeFlag()), HedgeFlagEnum.HF_Unknown);
			TradeTypeEnum tradeType = CtpConstant.tradeTypeMapReverse.getOrDefault(pTrade.getTradeType(), TradeTypeEnum.TT_Unknown);
			PriceSourceEnum priceSource = CtpConstant.priceSourceMapReverse.getOrDefault(pTrade.getPriceSource(), PriceSourceEnum.PSRC_Unknown);

			Trade trade = Trade.builder()
					.tradeDate(tradeDate)
					.tradeTime(tradeTime)
					.tradingDay(tradingDay)
					.tradeTimestamp(tradeTimestamp)
					.direction(direction)
					.offsetFlag(offsetFlag)
					.contract(Contract.builder().symbol(symbol).build())
					.orderId(originOrderId)
					.originOrderId(originOrderId)
					.price(price)
					.volume(volume)
					.gatewayId(gatewayId)
					.tradeType(tradeType)
					.priceSource(priceSource)
					.build();

			if (instrumentQueried.get()) {
				Contract contract = gatewayAdapter.mktCenter.getContract(ChannelType.CTP, symbol).contract();
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.TRADE, trade.toBuilder().contract(contract).build());
			} else {
				tradeBuilderCacheList.add(trade);
			}
			logger.info("{} 成交回报：合约{}，单号{}，方向{}，开平{}，价格{}，手数{}，交易日{}，类型{} & {}", logInfo,
					symbol, originOrderId, direction, offsetFlag, price, volume, tradingDay, hedgeFlag, tradeType);
		} catch (Throwable t) {
			logger.error("{} OnRtnTrade Exception： {}", logInfo, JSON.toJSONString(pTrade), t);
		}
	}

	// 发单错误回报
	@Override
	public void OnErrRtnOrderInsert(CThostFtdcInputOrderField pInputOrder, CThostFtdcRspInfoField pRspInfo) {
		try {
			if(logger.isErrorEnabled()) {				
				logger.error("{} 交易接口发单错误回报（OnErrRtnOrderInsert） 错误ID：{}，错误信息：{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
				logger.error("{} 交易接口发单错误回报（OnErrRtnOrderInsert） 委托单详细信息 -> {}", logInfo, JSON.toJSONString(pInputOrder));
			}
			
			SubmitOrderReq orderReq = orderReqMap.get(StringUtils.trim(pInputOrder.getOrderRef()));
			if(orderReq != null) {
				HedgeFlagEnum hedgeFlag = CtpConstant.hedgeFlagMapReverse.getOrDefault(pInputOrder.getCombHedgeFlag(), HedgeFlagEnum.HF_Unknown);
				String errMsg = pRspInfo != null ? pRspInfo.getErrorMsg() : "";
				
				gatewayAdapter.getEventEngine().emitEvent(
						NorthstarEventType.ORDER, 
						toOrderBuilder(orderReq)
							.orderDate(LocalDate.now())
							.orderTime(LocalTime.now())
							.hedgeFlag(hedgeFlag)
							.orderStatus(OrderStatusEnum.OS_Rejected)
							.statusMsg(errMsg)
							.build());
			}

			if (instrumentQueried.get() && pRspInfo != null) {
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.NOTICE, Notice.builder()
						.content(logInfo + "交易接口发单错误回报，错误ID:" + pRspInfo.getErrorID() + "，错误信息:" + pRspInfo.getErrorMsg())
						.status(CommonStatusEnum.COMS_ERROR)
						.build());
			}
		} catch (Throwable t) {
			logger.error("{} OnErrRtnOrderInsert Exception", logInfo, t);
		}
	}
	
	private Order.OrderBuilder toOrderBuilder(SubmitOrderReq orderReq){
		return Order.builder()
				.gatewayId(orderReq.gatewayId())
				.originOrderId(orderReq.originOrderId())
				.contract(orderReq.contract())
				.direction(orderReq.direction())
				.offsetFlag(orderReq.offsetFlag())
				.forceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.contingentCondition(orderReq.contingentCondition())
				.gtdDate(orderReq.gtdDate())
				.orderPriceType(orderReq.orderPriceType())
				.minVolume(orderReq.minVolume())
				.totalVolume(orderReq.volume())
				.price(orderReq.price())
				.timeCondition(orderReq.timeCondition())
				.stopPrice(orderReq.stopPrice())
				.volumeCondition(orderReq.volumeCondition());
	}

	// 撤单错误回报
	@Override
	public void OnErrRtnOrderAction(CThostFtdcOrderActionField pOrderAction, CThostFtdcRspInfoField pRspInfo) {
		if (pRspInfo != null) {
			logger.error("{} 交易接口撤单错误(OnErrRtnOrderAction) 错误ID:{},错误信息:{}", logInfo, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
			if (instrumentQueried.get()) {
				gatewayAdapter.getEventEngine().emitEvent(NorthstarEventType.NOTICE, Notice.builder()
						.content(logInfo + "交易接口撤单错误回报，错误ID:" + pRspInfo.getErrorID() + "，错误信息:" + pRspInfo.getErrorMsg())
						.status(CommonStatusEnum.COMS_ERROR)
						.build());
			}
		} else {
			logger.error("{} 处理交易接口撤单错误(OnErrRtnOrderAction)错误,无有效信息", logInfo);
		}
	}

}