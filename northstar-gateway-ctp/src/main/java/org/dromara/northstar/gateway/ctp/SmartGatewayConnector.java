package org.dromara.northstar.gateway.ctp;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import org.dromara.northstar.common.utils.CommonUtils;

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SmartGatewayConnector {

	private final ExecutorService exec;
	
	private List<Entry> hosts;
	
	public SmartGatewayConnector(List<String> hosts){
		this.hosts = hosts.stream().map(Entry::new).toList();
		this.exec = CommonUtils.newThreadPerTaskExecutor(getClass());
	}
	
	public String bestEndpoint() {
		if(hosts.size() == 1) {
			return hosts.get(0).endpoint;
		}
		for(Entry e : hosts) {
			e.reset();
			exec.execute(e::test);
		}
		for(int i=0; i < 50; i++) {
			if(hosts.stream().filter(e -> e.delay < Integer.MAX_VALUE).count() > 0) {
				Optional<Entry> bestEntry = hosts.stream().sorted().findFirst();
				if(bestEntry.isPresent()) {
					return bestEntry.get().endpoint;
				}
			}
			try {
				Thread.sleep(100);	//每100毫秒检测一次
			} catch (InterruptedException ex) {
				log.error("", ex);
			}
		}
		return hosts.get(0).endpoint;
	}
	
	@EqualsAndHashCode
	private class Entry implements Comparable<Entry>{
		
		private String endpoint;
		private int delay = Integer.MAX_VALUE;
		
		Entry(String endpoint){
			this.endpoint = endpoint;
		}
		
		@Override
		public int compareTo(Entry o) {
			return delay < o.delay ? -1 : 1;
		}
		
		public void reset() {
			delay = Integer.MAX_VALUE;
		}
		
		public void test() {
			int count = 3;
			try {				
				int[] testResults = new int[count];
				for(int i=0; i<count; i++) {					
					InetAddress geek = InetAddress.getByName(endpoint);
					long startTime = System.currentTimeMillis();
					if(geek.isReachable(5000)) {
						testResults[i] = (int) (System.currentTimeMillis() - startTime);
					} else {
						testResults[i] = 5000;
					}
				}
				delay = IntStream.of(testResults).sum() / count;
				log.debug("[{}] 连线用时：{}毫秒", endpoint, delay);
			} catch (IOException e) {
				log.error("无法测试IP：" + endpoint, e);
			}
		}
	}
}
