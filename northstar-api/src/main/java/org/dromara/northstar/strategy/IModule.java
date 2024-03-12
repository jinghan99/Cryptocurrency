package org.dromara.northstar.strategy;

import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.core.Contract;

/**
 * 模组抽象接口
 * @author KevinHuangwl
 *
 */
public interface IModule {

	/**
	 * 模组名称
	 * @return
	 */
	String getName();
	/**
	 * 设置运行状态
	 * @param enabled
	 */
	void setEnabled(boolean enabled);
	/**
	 * 获取运行状态
	 * @return
	 */
	boolean isEnabled();
	/**
	 * 监听事件
	 * @param event
	 */
	void onEvent(NorthstarEvent event);
	/**
	 * 获取合约关联的交易账户
	 * @param contract
	 * @return
	 */
	IAccount getAccount(Contract contract);
	/**
	 * 获取模组状态描述
	 * @return
	 */
	ModuleRuntimeDescription getRuntimeDescription();
	/**
	 * 获取模组描述
	 * @return
	 */
	ModuleDescription getModuleDescription();
	/**
	 * 获取模组上下文
	 * @return
	 */
	IModuleContext getModuleContext();
	/**
	 * 获取交易策略
	 * @return
	 */
	TradeStrategy getTradeStrategy();
}
