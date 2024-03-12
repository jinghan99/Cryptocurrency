package org.dromara.northstar.event;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 此类为内部事件分发器，统一处理FastEventEngine的事件分发
 * @auth KevinHuangwl
 */
@Component
public class InternalDispatcher implements NorthstarEventDispatcher {

	@Autowired
	private ModuleHandler moduleHandler;
	@Autowired
	private SimMarketHandler simMarketHandler;
	@Autowired
	private AccountHandler accountHandler;
	@Autowired
	private ConnectionHandler connHandler;
	@Autowired
	private EventNotificationHandler notificationHandler;
	@Autowired
	private MarketDataHandler mdHandler;
	@Autowired
	private BroadcastHandler bcHandler;
	@Autowired
	private IllegalOrderHandler illOrderHandler;
	
	
	public InternalDispatcher(FastEventEngine feEngine) {
		feEngine.addHandler(this);
	}
	
	@Override
	public void onEvent(NorthstarEvent event, long sequence, boolean endOfBatch) throws Exception {
		// 按优先级进行事件分发
		moduleHandler.onEvent(event);
		simMarketHandler.onEvent(event);
		bcHandler.onEvent(event);
		accountHandler.onEvent(event);
		connHandler.onEvent(event);
		mdHandler.onEvent(event);
		illOrderHandler.onEvent(event);
		notificationHandler.onEvent(event);
	}

}
