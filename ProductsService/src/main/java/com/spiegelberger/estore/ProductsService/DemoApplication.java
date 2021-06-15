package com.spiegelberger.estore.ProductsService;


import org.axonframework.commandhandling.CommandBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.ApplicationContext;

import com.spiegelberger.estore.ProductsService.command.interceptors.CreateProductCommandInterceptor;

@EnableEurekaClient
@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
	
	@Autowired
	public void registerCreateProductCommandInterceptor(ApplicationContext context,
															CommandBus commandBus) {
					
		commandBus.registerDispatchInterceptor(context.getBean(CreateProductCommandInterceptor.class));
	}

}
