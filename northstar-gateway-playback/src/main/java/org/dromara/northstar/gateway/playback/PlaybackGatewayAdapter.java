package org.dromara.northstar.gateway.playback;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.gateway.MarketGateway;

public class PlaybackGatewayAdapter implements MarketGateway {
	
	private IPlaybackContext ctx;
	
	private GatewayDescription gd;
	
	private ConnectionState connState = ConnectionState.DISCONNECTED;
	
	public PlaybackGatewayAdapter(IPlaybackContext ctx, GatewayDescription gd) {
		this.ctx = ctx;
		this.gd = gd;
		ctx.onStopCallback(() -> connState = ConnectionState.DISCONNECTED);
	}

	@Override
	public void connect() {
		connState = ConnectionState.CONNECTED;
		ctx.start();
	}

	@Override
	public void disconnect() {
		ctx.stop();
		connState = ConnectionState.DISCONNECTED;
	}
	
	@Override
	public ConnectionState getConnectionState() {
		return connState;
	}

	@Override
	public boolean getAuthErrorFlag() {
		return false;
	}

	@Override
	public boolean subscribe(Contract contract) {
		// 动态订阅不需要实现
		return true;
	}

	@Override
	public boolean unsubscribe(Contract contract) {
		// 动态取消订阅不需要实现
		return true;
	}

	@Override
	public boolean isActive() {
		return ctx.isRunning();
	}

	@Override
	public ChannelType channelType() {
		return ChannelType.PLAYBACK;
	}

	@Override
	public GatewayDescription gatewayDescription() {
		gd.setConnectionState(getConnectionState());
		return gd;
	}

	@Override
	public String gatewayId() {
		return gd.getGatewayId();
	}

}
