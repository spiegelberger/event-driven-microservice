package com.spiegelberger.estore.OrdersService.saga;

import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.CommandResultMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import com.spiegelberger.estore.OrdersService.core.events.OrderCreatedEvent;
import com.spiegelberger.estore.core.commands.ReserveProductCommand;
import com.spiegelberger.estore.core.events.ProductReservedEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Saga
public class OrderSaga {

	@Autowired
	private transient CommandGateway commandGateway;
	
	@StartSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderCreatedEvent orderCreatedEvent) {
		
		ReserveProductCommand reserveProductCommand = ReserveProductCommand.builder()
				.orderId(orderCreatedEvent.getAddressId())
				.productId(orderCreatedEvent.getProductId())
				.quantity(orderCreatedEvent.getQuantity())
				.userId(orderCreatedEvent.getUserId())
				.build();

		log.info("OrderCreatedEvent handled for orderId: " + reserveProductCommand.getOrderId() + 
				" and productId: " + reserveProductCommand.getProductId() );
		
		commandGateway.send(reserveProductCommand, new CommandCallback<ReserveProductCommand, Object>(){
			
			public void onResult(CommandMessage<? extends ReserveProductCommand> commandMessage,
					CommandResultMessage<?extends Object> commandResultMessage){
					
				if(commandResultMessage.isExceptional()) {
					// Start a compensating transaction					
				}
			}
			
		});
		
	}
	
	@SagaEventHandler(associationProperty="orderId")
	public void handle(ProductReservedEvent productReservedEvent) {
		
		// Process user payment
		log.info("productReservedEvent is called for productId: " + productReservedEvent.getProductId() + 
				" and orderId: " + productReservedEvent.getOrderId());
	}
		
}
