package org.dromara.northstar.web.service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.LogDescription;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.strategy.IModule;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import cn.hutool.core.io.FileUtil;

public class LogService {
	
	private static final String NORTHSTAR_ROOT = "org.dromara.northstar";
	
	private LoggingSystem loggingSystem;
	
	private ModuleManager moduleMgr;
	
	public LogService(LoggingSystem logginSystem, ModuleManager moduleMgr) {
		this.loggingSystem = logginSystem;
		this.moduleMgr = moduleMgr;
	}
	
	public LogDescription tailLogFile(File logFile, long positionOffset, int tailNumOfLines) throws IOException {
		LinkedList<String> list = new LinkedList<>();
		LogDescription result = new LogDescription();
		try(RandomAccessFile raf = new RandomAccessFile(logFile, "r")){
			long lenOfFile = raf.length();
			long realOffset = Math.max(positionOffset, lenOfFile - 1000000);	//最多加载1MB的日志数据
			result.setStartPosition(realOffset); 	
			result.setEndPosition(lenOfFile);
			raf.seek(realOffset);
			while(raf.getFilePointer() < lenOfFile) {
				String line = FileUtil.readLine(raf, StandardCharsets.UTF_8);
				if(StringUtils.isNotEmpty(line)) {					
					list.add(line);
				}
			}
			
			result.setLinesOfLog(list.subList(Math.max(0, list.size() - tailNumOfLines), list.size()));
			return result;
		}
	}
	
	public void setPlatformLogLevel(LogLevel level) {
		loggingSystem.setLogLevel(NORTHSTAR_ROOT, level);
	}
	
	public void setModuleLogLevel(String moduleName, LogLevel level) {
		IModule module = moduleMgr.get(Identifier.of(moduleName));
		Logger logger = (Logger) module.getModuleContext().getLoggerFactory().getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.toLevel(level.toString()));
	}
	
	public LogLevel getPlatformLogLevel() {
		return loggingSystem.getLoggerConfiguration(NORTHSTAR_ROOT).getEffectiveLevel();
	}
	
	public LogLevel getModuleLogLevel(String moduleName) {
		IModule module = moduleMgr.get(Identifier.of(moduleName));
		Logger logger = (Logger) module.getModuleContext().getLoggerFactory().getLogger(Logger.ROOT_LOGGER_NAME);
		return LogLevel.valueOf(logger.getLevel().toString());
	}
}
