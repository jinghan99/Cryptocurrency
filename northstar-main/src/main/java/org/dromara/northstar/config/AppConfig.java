package org.dromara.northstar.config;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.GlobalSpringContext;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.utils.LocalEnvUtils;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.web.interceptor.AuthorizationInterceptor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.corundumstudio.socketio.SocketIOServer;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

/**
 * 配置转换器
 * 
 * @author KevinHuangwl
 *
 */
@Slf4j
@Configuration
public class AppConfig implements WebMvcConfigurer, InitializingBean, DisposableBean {

	@Autowired
	private SocketIOServer socketServer;
	
	@Value("${spring.profiles.active}")
	private String profile;
	
	@Autowired
	private Environment env;
	
	@Autowired
	private ApplicationContext springCtx;
	
	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		/**
		 * 调整转换器优先级
		 */
		List<HttpMessageConverter<?>> jacksonConverters = new ArrayList<>();
		Iterator<HttpMessageConverter<?>> itCvt = converters.iterator();
		while (itCvt.hasNext()) {
			HttpMessageConverter<?> cvt = itCvt.next();
			if (cvt instanceof MappingJackson2HttpMessageConverter) {
				jacksonConverters.add(cvt);
				itCvt.remove();
			}
		}
		for (HttpMessageConverter<?> cvt : jacksonConverters) {
			converters.add(0, cvt);
		}
	}

    @Bean
    CorsFilter corsFilter() {

        CorsConfiguration config = new CorsConfiguration();
        // 设置允许跨域请求的域名
        config.addAllowedOriginPattern("*");
        // 是否允许证书 不再默认开启
        config.setAllowCredentials(true);
        // 设置允许的方法
        config.addAllowedMethod("*");
        // 允许任何头
        config.addAllowedHeader("*");
        config.addExposedHeader("token");
        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
        configSource.registerCorsConfiguration("/**", config);
        return new CorsFilter(configSource);
    }

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		if(!StringUtils.equals(profile, "e2e")) {
			registry.addInterceptor(new AuthorizationInterceptor()).addPathPatterns("/**").excludePathPatterns("/auth/login");
		}
	}

    @Bean
    IMarketCenter marketCenter(FastEventEngine fastEventEngine) throws IOException {
        return new MarketCenter(fastEventEngine);
    }

	private static OkHttpClient getUnsafeOkHttpClient() {
		try {
			// Create a trust manager that does not validate certificate chains
			final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new java.security.cert.X509Certificate[] {};
				}
			} };

			// Install the all-trusting trust manager
			final SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			// Create an ssl socket factory with our all-trusting manager
			final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
			return new OkHttpClient.Builder()
					.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
					.hostnameVerifier((host, session) -> true)
					.retryOnConnectionFailure(true)
					.build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .requestFactory(() -> new OkHttp3ClientHttpRequestFactory(getUnsafeOkHttpClient()))
                .setReadTimeout(Duration.ofSeconds(60))
                .setConnectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    @ConditionalOnExpression("systemEnvironment['IDEA_INITIAL_DIRECTORY'] == null")
    CommandLineRunner printVersionInfo(BuildProperties buildProperties) {
    	return args -> log.info("Version: {}, Build Time: {}", buildProperties.getVersion(), buildProperties.getTime().atOffset(ZoneOffset.ofHours(8)));
	}

	@Override
	public void destroy() throws Exception {
		socketServer.stop();		
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		LocalEnvUtils.setEnvironment(env);
		log.info("设置全局环境信息");
		GlobalSpringContext.INSTANCE.set(springCtx);
	}

}