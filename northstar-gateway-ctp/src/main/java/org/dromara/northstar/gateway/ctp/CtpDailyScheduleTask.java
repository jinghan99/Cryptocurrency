package org.dromara.northstar.gateway.ctp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.gateway.Gateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CtpDailyScheduleTask {

	@Autowired
	private ObjectManager<Gateway> gatewayMgr;
	
	@Autowired
	private CtpHolidayManager holidayMgr;
	
	/**
	 * 网关开盘前连线检查
	 * @throws InterruptedException
	 */
	@Scheduled(cron="0 55 8,20 ? * 1-5")
	public void dailyConnection() throws InterruptedException {
		if(holidayMgr.isHoliday(LocalDateTime.now())) {
			log.info("当前为假期，不执行定时连线任务");
			return;
		}
		log.info("定时连线任务");
		connectIfNotConnected();
	}
	/**
	 * 网关定时断开
	 * @throws InterruptedException
	 */
	@Scheduled(cron="0 31 2,15 ? * 1-6")
	public void dailyDisconnection() throws InterruptedException {
		if(holidayMgr.isHoliday(LocalDateTime.now()) && LocalDate.now().getDayOfWeek() != DayOfWeek.SATURDAY) {
			log.info("当前为假期，不执行定时离线任务");
			return;
		}
		log.info("定时离线任务");
		gatewayMgr.findAll().stream()
			.filter(gw -> gw.gatewayDescription().getChannelType() == ChannelType.CTP)
			.filter(gw -> gw.getConnectionState() == ConnectionState.CONNECTED)
			.forEach(Gateway::disconnect);
	}
	/**
	 * 网关响应速度检查
	 */
	@Scheduled(cron="0 0/15 0-1,9-14,21-23 ? * 1-5")
	public void timelyCheckConnection() {
		if(holidayMgr.isHoliday(LocalDateTime.now())) {
			return;
		}
		log.debug("开盘时间连线巡检");
		connectIfNotConnected();
	}
	
	private void connectIfNotConnected() {
		gatewayMgr.findAll().stream()
			.filter(gw -> gw.gatewayDescription().getChannelType() == ChannelType.CTP)
			.filter(gw -> gw.gatewayDescription().isAutoConnect())
			.filter(gw -> gw.getConnectionState() != ConnectionState.CONNECTED)
			.forEach(Gateway::connect);
	}
}
