package org.dromara.northstar.support.utils;

import java.util.List;
import java.util.stream.Collectors;

import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.strategy.IModule;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;

/**
 * 持仓检查器
 * @author KevinHuangwl
 *
 */
@Slf4j
public class PositionChecker {
	
	private ModuleManager moduleMgr;

	public PositionChecker(ModuleManager moduleMgr) {
		this.moduleMgr = moduleMgr;
	}
	
	/**
	 * 检查持仓相等
	 * @param position	某合约某个方向的物理持仓
	 */
	public void checkPositionEquivalence(Position position) {
		DirectionEnum direction = switch(position.positionDirection()) {
		case PD_Long -> DirectionEnum.D_Buy;
		case PD_Short -> DirectionEnum.D_Sell;
		default -> throw new IllegalArgumentException("Unexpected value: " + position.positionDirection());
		};
		List<IModule> modules = moduleMgr.allModules()
									.stream()
									.filter(m -> hasLinkageBetweenModuleAndPosition(m, position))
									.filter(m -> m.getModuleDescription().getModuleAccountSettingsDescription()
											.stream()
											.flatMap(mad -> mad.getBindedContracts().stream())
											.map(ContractSimpleInfo::getUnifiedSymbol)
											.collect(Collectors.toSet()).contains(position.contract().unifiedSymbol()))
									.toList();
		int totalPosition = modules.stream()
									.mapToInt(m -> m.getModuleContext().getModuleAccount().getNonclosedPosition(position.contract(), direction))
									.sum();
		if(totalPosition != position.position()) {
			log.warn("{} {}头 实际持仓数：{}", position.contract().name(), FieldUtils.chn(direction), position.position());
			modules.forEach(m -> 
				log.warn("模组 [{}] {}头 持仓为：{}", m.getName(), FieldUtils.chn(direction), 
						m.getModuleContext().getModuleAccount().getNonclosedPosition(position.contract(), direction))
			);
			throw new IllegalStateException(String.format("[%s %s %s头] 逻辑持仓与实际持仓不一致。逻辑持仓数：%d，实际持仓数：%d", 
					position.gatewayId(), position.contract().name(), FieldUtils.chn(direction), totalPosition, position.position()));
		}
	}
	
	private boolean hasLinkageBetweenModuleAndPosition(IModule module, Position position) {
		return module.getModuleDescription().getModuleAccountSettingsDescription()
				.stream()
				.map(mad -> mad.getAccountGatewayId())
				.collect(Collectors.toSet()).contains(position.gatewayId());
	}
}
