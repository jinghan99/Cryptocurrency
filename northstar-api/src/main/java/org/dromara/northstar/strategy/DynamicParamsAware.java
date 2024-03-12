package org.dromara.northstar.strategy;

import org.dromara.northstar.common.model.DynamicParams;

public interface DynamicParamsAware {

	/**
	 * 获取配置类
	 * @return
	 */
	DynamicParams getDynamicParams();
	
	/**
	 * 通过配置类初始化
	 * @param params
	 */
	void initWithParams(DynamicParams params);
}
