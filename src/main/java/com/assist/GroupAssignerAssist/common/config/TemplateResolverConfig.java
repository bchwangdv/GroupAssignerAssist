package com.assist.GroupAssignerAssist.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class TemplateResolverConfig {

	@Bean
	public ClassLoaderTemplateResolver dotResolver() {
		ClassLoaderTemplateResolver dot = new ClassLoaderTemplateResolver();
		dot.setPrefix("templates/views/");
		dot.setSuffix(".html");
		dot.setTemplateMode(TemplateMode.HTML);
		dot.setCharacterEncoding("UTF-8");
		dot.setCacheable(false);
		dot.setCheckExistence(true);
		
		return dot;
	}
}
