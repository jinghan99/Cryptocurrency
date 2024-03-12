package org.dromara.northstar.gateway;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

/**
 * （可交易的）投资品种
 * @author KevinHuangwl
 *
 */
public interface Instrument {

	/**
	 * 名称
	 * @return
	 */
	String name();
	
	/**
	 * 唯一标识
	 * @return
	 */
	Identifier identifier();
	
	/**
	 * 种类
	 * @return
	 */
	ProductClassEnum productClass();
	
	/**
	 * 交易所
	 * @return
	 */
	ExchangeEnum exchange();
	
	/**
	 * 网关渠道类型
	 * @return
	 */
	ChannelType channelType();
	
	/**
	 * 数据源
	 * @return
	 */
	default IDataSource dataSource() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * 设置合约定义
	 */
	default void setContractDefinition(ContractDefinition contractDef) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * 转换为合约信息
	 * @return
	 */
	default Contract contract() {
		throw new UnsupportedOperationException();
	}
}
