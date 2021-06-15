package com.spiegelberger.estore.ProductsService.command.interceptors;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiFunction;

import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.springframework.stereotype.Component;

import com.spiegelberger.estore.ProductsService.command.CreateProductCommand;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CreateProductCommandInterceptor implements 
						MessageDispatchInterceptor<CommandMessage<?>>{

	@Override
	public BiFunction<Integer, CommandMessage<?>, CommandMessage<?>> handle(
			List<? extends CommandMessage<?>> messages) {
		
		return (index, command)->{
			
			log.info("Intercepted command: " + command.getPayloadType());
			
			if(CreateProductCommand.class.equals(command.getPayloadType())) {
				
				CreateProductCommand createCProductCommand = (CreateProductCommand)command.getPayload();
				
				if(createCProductCommand.getPrice().compareTo(BigDecimal.ZERO)<=0) {
					throw new IllegalArgumentException("Price cannot be less or equal to zero.");
				}
				
				if(createCProductCommand.getTitle()==null || createCProductCommand.getTitle().isBlank()) {
					throw new IllegalArgumentException("Title cannot be empty");
				}
			}
			return command;
		};
	}

}
