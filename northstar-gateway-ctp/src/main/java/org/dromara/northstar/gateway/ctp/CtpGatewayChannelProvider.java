package org.dromara.northstar.gateway.ctp;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.dromara.northstar.common.SettingOptionsProvider;
import org.dromara.northstar.common.model.OptionItem;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户想要定义自定义的CTP渠道，可以通过定义一个ctp-channels.json并放置在与CTP网关包同级部署目录下
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
public class CtpGatewayChannelProvider implements SettingOptionsProvider {

	@Override
	public List<OptionItem> optionVals() {
		return brokerList().stream().map(broker -> new OptionItem(broker.getName(), broker.getName())).toList();
	}

	public List<CtpGatewaySettings.Broker> brokerList(){
		URL fileSrc = this.getClass().getClassLoader().getResource("ctp-channels.json");
		if(Objects.isNull(fileSrc)) {
			String defFilePath = System.getenv(CtpConstants.CTP_CHANNEL_FILE);
			if(Objects.nonNull(defFilePath)) {				
				try {
					fileSrc = new URL("file", "", slashify(defFilePath, false));
				} catch (MalformedURLException e) {
					log.error("无法加载ctp-channels.json", e);
				}
			}
		}
		List<CtpGatewaySettings.Broker> brokers = new ArrayList<>();
		ObjectMapper objectMapper = new ObjectMapper();
		if(Objects.nonNull(fileSrc)) {
			try {
				List<CtpGatewaySettings.Broker> customBrokers = objectMapper.readValue(fileSrc, new TypeReference<List<CtpGatewaySettings.Broker>>() {});
				brokers.addAll(customBrokers);
			} catch (IOException e) {
				log.error("无法加载ctp-channels.json", e);
			}
		}
		return brokers;
	}

	private static String slashify(String path, boolean isDirectory) {
		String p = path;
		if (File.separatorChar != '/')
			p = p.replace(File.separatorChar, '/');
		if (!p.startsWith("/"))
			p = "/" + p;
		if (!p.endsWith("/") && isDirectory)
			p = p + "/";
		return p;
	}

}
