package org.dromara.northstar.gateway.ctp.x64v6v3v15v;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.assertj.core.data.Offset;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.Account;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Notice;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.gateway.ctp.CtpContractDefProvider;
import org.dromara.northstar.gateway.ctp.CtpGatewayChannelProvider;
import org.dromara.northstar.gateway.ctp.CtpGatewaySettings;
import org.dromara.northstar.gateway.ctp.CtpGatewaySettings.Broker;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;

class CtpGatewayAdapterTest {
	
	static Logger logger = (Logger) LoggerFactory.getLogger("ROOT");

	static String TEST_BROKER = "平安测试";
	static CtpContractDefProvider contractDefPvd = new CtpContractDefProvider();
	static CtpGatewayChannelProvider channelPvd = new CtpGatewayChannelProvider();
	static CtpGatewayAdapter mdAdapter;
	static CtpGatewayAdapter tdAdapter;
	static TestEngine feEngine = new TestEngine();
	
	static Map<String, Broker> brokerMap = channelPvd.brokerList().stream().collect(Collectors.toMap(Broker::getName, b -> b));
	
	static SubmitOrderReq protoOrderReq = SubmitOrderReq.builder()
			.gatewayId("testGateway")
			.volumeCondition(VolumeConditionEnum.VC_AV)
			.timeCondition(TimeConditionEnum.TC_GFD)
			.orderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
			.contingentCondition(ContingentConditionEnum.CC_Immediately)
			.actionTimestamp(System.currentTimeMillis())
			.minVolume(1)
			.build();
	
	static MarketCenter mktCenter = new MarketCenter(feEngine);
	static GatewayDescription mdgd = GatewayDescription.builder()
			.gatewayUsage(GatewayUsage.MARKET_DATA)
			.channelType(ChannelType.CTP)
			.gatewayId("md")
			.settings(CtpGatewaySettings.builder().userId(System.getenv("CTP_TEST_USER")).password(System.getenv("CTP_TEST_PWD")).broker(brokerMap.get(TEST_BROKER)).build())
			.build();
	
	static GatewayDescription tdgd = GatewayDescription.builder()
			.gatewayUsage(GatewayUsage.TRADE)
			.channelType(ChannelType.CTP)
			.gatewayId("td")
			.settings(CtpGatewaySettings.builder().userId(System.getenv("CTP_TEST_USER")).password(System.getenv("CTP_TEST_PWD")).broker(brokerMap.get(TEST_BROKER)).build())
			.build();
	
	// 测试用例1：交易网关连线与断开
	@BeforeAll
	static void prepare() throws InterruptedException {
		logger.setLevel(Level.INFO);
		
		mktCenter.addDefinitions(contractDefPvd.get());
		tdAdapter = new CtpGatewayAdapter(feEngine, tdgd, mktCenter);
		tdAdapter.connect();
		mdAdapter = new CtpGatewayAdapter(feEngine, mdgd, mktCenter);
		mdAdapter.connect();
		Thread.sleep(10000);
		mdAdapter.subscribe(Contract.builder().symbol("rb2405").build());
		Thread.sleep(1000);
	}
	
	@AfterAll
	static void cleanup() {
		mdAdapter.disconnect();
		tdAdapter.disconnect();
	}
	
	// 测试用例2：发单，成交
	@Test
	void testSubmitOrderAndTrade() throws InterruptedException {
		Contract contarct = mktCenter.getContract(ChannelType.CTP, "rb2405").contract();
		feEngine.orders.clear();
		feEngine.trades.clear();
		feEngine.notices.clear();
		String orderId = UUID.randomUUID().toString();
		tdAdapter.submitOrder(protoOrderReq.toBuilder()
				.originOrderId(orderId)
				.contract(contarct)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.volume(1)
				.price(feEngine.lastTick.upperLimit())
				.build());
		
		Thread.sleep(1000);
		assertThat(feEngine.orders.stream().filter(od -> od.originOrderId().equals(orderId)).toList()).isNotEmpty();
		assertThat(feEngine.trades.stream().filter(tr -> tr.originOrderId().equals(orderId)).toList()).isNotEmpty();
		assertThat(feEngine.notices).isEmpty();
	}
	
	// 测试用例3：发单，撤单
	@Test
	void testSubmitOrderAndCancelOrder() throws InterruptedException {
		Contract contarct = mktCenter.getContract(ChannelType.CTP, "rb2405").contract();
		feEngine.orders.clear();
		feEngine.trades.clear();
		feEngine.notices.clear();
		String orderId = UUID.randomUUID().toString();
		tdAdapter.submitOrder(protoOrderReq.toBuilder()
				.originOrderId(orderId)
				.contract(contarct)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.volume(1)
				.price(feEngine.lastTick.lowerLimit())
				.build());
		
		Thread.sleep(1000);
		tdAdapter.cancelOrder(orderId);
		Thread.sleep(1000);
		assertThat(feEngine.orders.stream()
				.filter(od -> od.originOrderId().equals(orderId))
				.filter(od -> od.orderStatus() == OrderStatusEnum.OS_Canceled)
				.toList()).isNotEmpty();
		assertThat(feEngine.trades).isEmpty();
		assertThat(feEngine.notices).isEmpty();
	}
	
	// 测试用例4：查询账户信息
	@Test
	void testQryAccount() {
		assertThat(feEngine.account).isNotNull();
		assertThat(feEngine.account.updateTimestamp()).isCloseTo(System.currentTimeMillis(), Offset.offset(3000L));
	}
	
	// 测试用例5：查询持仓信息
	@Test
	void testQryPosition() {
		Contract c = mktCenter.getContract(ChannelType.CTP, "rb2405").contract();
		assertThat(feEngine.posTable.size()).isPositive();
		assertThat(feEngine.posTable.get(PositionDirectionEnum.PD_Long, c)).isNotNull();
		assertThat(feEngine.posTable.get(PositionDirectionEnum.PD_Long, c).updateTimestamp()).isCloseTo(System.currentTimeMillis(), Offset.offset(3000L));
	}
	
	// 测试用例6：发单，废单
	@Test
	void testRejectOrder() throws InterruptedException {
		Contract contarct = mktCenter.getContract(ChannelType.CTP, "rb2405").contract();
		feEngine.orders.clear();
		feEngine.trades.clear();
		feEngine.notices.clear();
		String orderId = UUID.randomUUID().toString();
		tdAdapter.submitOrder(protoOrderReq.toBuilder()
				.originOrderId(orderId)
				.contract(contarct)
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Close)
				.volume(1)
				.price(feEngine.lastTick.upperLimit() + 1000)
				.build());
		
		Thread.sleep(1000);
		assertThat(feEngine.orders.stream()
				.filter(od -> od.originOrderId().equals(orderId))
				.filter(od -> od.orderStatus() == OrderStatusEnum.OS_Rejected)
				.toList()).isNotEmpty();
		assertThat(feEngine.trades).isEmpty();
		assertThat(feEngine.notices).isNotEmpty();
	}

	
	static class TestEngine implements FastEventEngine {

		Tick lastTick;
		
		Account account;
		
		Set<Order> orders = new HashSet<>();
		Set<Trade> trades = new HashSet<>();
		
		Set<Notice> notices = new HashSet<>();
		
		Table<PositionDirectionEnum, Contract, Position> posTable = HashBasedTable.create();

		@Override
		public void addHandler(NorthstarEventDispatcher handler) {
		}

		@Override
		public void removeHandler(NorthstarEventDispatcher handler) {
		}

		@Override
		public void emitEvent(NorthstarEventType event, Object obj) {
			if(obj instanceof Tick t) {
				lastTick = t;
				System.out.println("tick:" + JSON.toJSONString(t));
			} else if (obj instanceof Order o) {
				synchronized (orders) {					
					orders.add(o);
				}
				System.out.println("order:" + JSON.toJSONString(o));
			} else if (obj instanceof Trade t) {
				synchronized (trades) {					
					trades.add(t);
				}
				System.out.println("trade:" + JSON.toJSONString(t));
			} else if (obj instanceof Account acc) {
				account = acc;
				System.out.println("account:" + JSON.toJSONString(acc));
			} else if (obj instanceof Position pos) {
				synchronized (posTable) {					
					posTable.put(pos.positionDirection(), pos.contract(), pos);
				}
				System.out.println("position:" + JSON.toJSONString(pos));
			} else if (obj instanceof Notice not && not.status() == CommonStatusEnum.COMS_ERROR) {
				synchronized (notices) {					
					notices.add(not);
				}
			}
			
		}
		
	}
}
