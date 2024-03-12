package org.dromara.northstar.gateway.ctp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dromara.northstar.common.IHolidayManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.gateway.mktdata.NorthstarDataServiceDataSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;

/**
 * 法定节假日管理器
 * @author KevinHuangwl
 *
 */
@Slf4j
@Component
public class CtpHolidayManager implements IHolidayManager, InitializingBean{

	protected Set<LocalDate> holidaySet = new HashSet<>();

	@Autowired
	private NorthstarDataServiceDataSource dsMgr;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		LocalDate today = LocalDate.now();
		// 加载前后一年的假期数据
		addHoliday(dsMgr.getHolidays(ExchangeEnum.SHFE, LocalDate.of(today.getYear() - 1, 1, 1), LocalDate.of(today.getYear() - 1, 12, 31)));
		addHoliday(dsMgr.getHolidays(ExchangeEnum.SHFE, LocalDate.of(today.getYear(), 1, 1), LocalDate.of(today.getYear(), 12, 31)));
		addHoliday(dsMgr.getHolidays(ExchangeEnum.SHFE, LocalDate.of(today.getYear() + 1, 1, 1), LocalDate.of(today.getYear() + 1, 12, 31)));
	}
	
	private void addHoliday(List<LocalDate> holidays) {
		holidays.stream().forEach(date -> {
				log.debug("假期日：{}", date);
				holidaySet.add(date);
			});
	}
	
	@Override
	public boolean isHoliday(LocalDateTime dateTime) {
		LocalDate date = LocalDate.from(dateTime);
		boolean isWeekend = dateTime.getDayOfWeek().getValue() > 5;
		// 当天就是假期
		if(isWeekend || holidaySet.contains(date)) {
			return true;
		}
		// 当天不是假期的夜盘判断
		if(dateTime.getHour() >= 20) {
			boolean isFriday = dateTime.getDayOfWeek().getValue() == 5;
			date = LocalDate.from(dateTime.plusHours(isFriday ? 54 : 6));
		}
		if(dateTime.getHour() < 3) {
			date = LocalDate.from(dateTime.minusHours(4));
		}
		return holidaySet.contains(date) || date.getDayOfWeek().getValue() > 5;
	}

	@Override
	public ChannelType channelType() {
		return ChannelType.CTP;
	}
	
}
