package com.spiegelberger.estore.OrdersService.saga;


import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.CommandResultMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import com.spiegelberger.estore.OrdersService.core.events.OrderCreatedEvent;
import com.spiegelberger.estore.core.commands.ReserveProductCommand;
import com.spiegelberger.estore.core.events.ProductReservedEvent;
import com.spiegelberger.estore.core.model.User;
import com.spiegelberger.estore.core.query.FetchUserPaymentDetailsQuery;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Saga
public class OrderSaga {

	@Autowired
	private transient CommandGateway commandGateway;
	
	@Autowired
	private transient QueryGateway queryGateway;
	
	@StartSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderCreatedEvent orderCreatedEvent) {
		
		ReserveProductCommand reserveProductCommand = ReserveProductCommand.builder()
				.orderId(orderCreatedEvent.getOrderId())
				.productId(orderCreatedEvent.getProductId())
				.quantity(orderCreatedEvent.getQuantity())
				.userId(orderCreatedEvent.getUserId())
				.build();

		log.info("OrderCreatedEvent handled for orderId: " + reserveProductCommand.getOrderId() + 
				" and productId: " + reserveProductCommand.getProductId() );
		
		commandGateway.send(reserveProductCommand, new CommandCallback<ReserveProductCommand, Object>(){
			
			public void onResult(CommandMessage<? extends ReserveProductCommand> commandMessage,
					CommandResultMessage<? extends Object> commandResultMessage){
					
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
		
		FetchUserPaymentDetailsQuery fetchUserPaymentDetailsQuery = 
				new FetchUserPaymentDetailsQuery(productReservedEvent.getUserId());
		
		User userPaymentDetails = null;
		try {
			userPaymentDetails = 
					queryGateway.query(fetchUserPaymentDetailsQuery, ResponseTypes.instanceOf(User.class)).join();
		} catch (Exception ex) {
			log.error(ex.getMessage());
			
			//Start compensating transaction
			return;
			
		}
		
		if(userPaymentDetails == null) {
			//Start compensating transaction
			return;
		}
		
		log.info("Successfully fetched user payment details for user: " +  userPaymentDetails.getFirstName());
	}
		
}
