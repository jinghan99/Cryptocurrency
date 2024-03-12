package org.dromara.northstar.strategy.example;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.dromara.northstar.common.constant.FieldType;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Setting;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.strategy.AbstractStrategy;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.StrategicComponent;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.slf4j.Logger;

/**
 * ## 风险提示：该策略仅作技术分享，据此交易，风险自担 ##
 * @auth KevinHuangwl
 */
@StrategicComponent(DualAccountExampleStrategy.NAME)		// 该注解是用于给策略命名用的，所有的策略都要带上这个注解
public class DualAccountExampleStrategy extends AbstractStrategy implements TradeStrategy {

	protected static final String NAME = "示例-多账户简单策略";	// 之所以要这样定义一个常量，是为了方便日志输出时可以带上策略名称
	
	private InitParams params;	// 策略的参数配置信息
	
	private Logger logger;	// 为了使logger可以精细记录日志出处，需要保留一个私有对象
	
	/**
	 * 定义该策略的参数。该类每个策略必须自己重写一个，类名必须为InitParams，必须继承DynamicParams，必须是个static类。
	 * @author KevinHuangwl
	 */
	public static class InitParams extends DynamicParams {			// 每个策略都要有一个用于定义初始化参数的内部类，类名称不能改
		
		@Setting(label="操作间隔", type = FieldType.NUMBER, order=10, unit="秒")		// Label注解用于定义属性的元信息。可以声明单位
		private int actionInterval = 60;						// 属性可以为任意多个，当元素为多个时order值用于控制前端的显示顺序
		
	}
	
	/***************** 以下如果看不懂，基本可以照搬 *************************/
	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		this.params = (InitParams) params;
	}
	
	/***************** 以上如果看不懂，基本可以照搬 *************************/

	private long nextActionTime;
	
	private Set<Contract> symbolSet = new HashSet<>();
	
	private Contract chosen;
	
	@Override
	public void onTick(Tick tick) {
		symbolSet.add(tick.contract());
		long now = tick.actionTimestamp();
		// 启用后，等待10秒才开始交易
		if(nextActionTime == 0) {
			nextActionTime = now + 10000;
		}
		boolean flag = ThreadLocalRandom.current().nextBoolean();
		if(now > nextActionTime) {
			nextActionTime = now + params.actionInterval * 1000;
			logger.info("开始交易");
			if(ctx.getState().isEmpty()) {
				SignalOperation op = flag ? SignalOperation.BUY_OPEN : SignalOperation.SELL_OPEN;	// 随机开多或者开空
				chosen = randomPick();
				ctx.submitOrderReq(TradeIntent.builder()
						.contract(chosen)
						.operation(op)
						.volume(ctx.getDefaultVolume())
						.priceType(PriceType.WAITING_PRICE)
						.timeout(3000)
						.build());
				return;
			}
			if(ctx.getState() == ModuleState.HOLDING_LONG) {	
				ctx.submitOrderReq(TradeIntent.builder()
						.contract(chosen)
						.operation(SignalOperation.SELL_CLOSE)
						.priceType(PriceType.OPP_PRICE)
						.volume(ctx.getDefaultVolume())
						.timeout(3000)
						.build());
			}
			if(ctx.getState() == ModuleState.HOLDING_SHORT) {			
				ctx.submitOrderReq(TradeIntent.builder()
						.contract(chosen)
						.operation(SignalOperation.BUY_CLOSE)
						.priceType(PriceType.WAITING_PRICE)
						.volume(ctx.getDefaultVolume())
						.timeout(3000)
						.build());
			}
		}
	}
	
	private Contract randomPick() {
		int index = ThreadLocalRandom.current().nextInt(symbolSet.size());
		return symbolSet.stream().toList().get(index);
	}

	@Override
	public void setContext(IModuleContext context) {
		super.setContext(context);
		logger = context.getLogger(getClass());
	}

	@Override
	public String name() {
		return NAME;
	}

}
