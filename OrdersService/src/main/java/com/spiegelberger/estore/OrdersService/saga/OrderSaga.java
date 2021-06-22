package com.spiegelberger.estore.OrdersService.saga;


import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.CommandResultMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import com.spiegelberger.estore.OrdersService.command.commands.ApproveOrderCommand;
import com.spiegelberger.estore.OrdersService.command.commands.RejectOrderCommand;
import com.spiegelberger.estore.OrdersService.core.events.OrderApprovedEvent;
import com.spiegelberger.estore.OrdersService.core.events.OrderCreatedEvent;
import com.spiegelberger.estore.OrdersService.core.events.OrderRejectedEvent;
import com.spiegelberger.estore.core.commands.CancelProductReservationCommand;
import com.spiegelberger.estore.core.commands.ProcessPaymentCommand;
import com.spiegelberger.estore.core.commands.ReserveProductCommand;
import com.spiegelberger.estore.core.events.PaymentProcessedEvent;
import com.spiegelberger.estore.core.events.ProductReservationCancelledEvent;
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
			cancelProductReservation(productReservedEvent, ex.getMessage());
			return;
			
		}
		
		if(userPaymentDetails == null) {
			//Start compensating transaction
			cancelProductReservation(productReservedEvent, "Could not fetch user payment details");
			return;
		}
		
		log.info("Successfully fetched user payment details for user: " +  userPaymentDetails.getFirstName());
		
		// Process payment details
		ProcessPaymentCommand processPaymentCommand = ProcessPaymentCommand.builder()
				.orderId(productReservedEvent.getOrderId())
				.paymentDetails(userPaymentDetails.getPaymentDetails())
				.paymentId(UUID.randomUUID().toString())
				.build();
		
		String result = null;
		try {
			result = commandGateway.sendAndWait(processPaymentCommand, 10, TimeUnit.SECONDS);
		} catch (Exception ex) {
			log.error(ex.getMessage());
			// Start compensating transaction
			cancelProductReservation( productReservedEvent, ex.getMessage() );
			return;
		}
		
		if(result == null) {
			log.info("The ProcessPaymentCommand resulted in NULL. Initiating a compensating transaction");
			// Start compensating transaction
			cancelProductReservation( productReservedEvent, "Could not process user payment with payment details");
		}
	}
	
		
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(PaymentProcessedEvent paymentProcessedEvent) {
		
		// Send an ApproveOrderCommand
		ApproveOrderCommand approvedOrderCommand = 
				new ApproveOrderCommand(paymentProcessedEvent.getOrderId()); 
		
		commandGateway.send(approvedOrderCommand);
	}
	
	@EndSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderApprovedEvent orderApprovedEvent) {
		
		log.info("Order is approved. Order Saga is complete for orderId: " + 
							orderApprovedEvent.getOrderId() );
	// This can be also used instead of @EndSaga:
	//	SagaLifecycle.end();
	}
	
	
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(ProductReservationCancelledEvent  productReservationCancelledEvent) {
		
		// Create and send a RejectOrderCommand
		RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(
			productReservationCancelledEvent.getOrderId(), productReservationCancelledEvent.getReason() );
		
		commandGateway.send(rejectOrderCommand);
	}
	
	
	@EndSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderRejectedEvent orderRejectedEvent) {
		
		log.info("Successfully rejected order with id: " + orderRejectedEvent.getOrderId());
	}
	
	
	private void cancelProductReservation(ProductReservedEvent productReservedEvent, String reason) {
		
		CancelProductReservationCommand publishProductReservationCommand =
				CancelProductReservationCommand.builder()
				.orderId(productReservedEvent.getOrderId())
				.productId(productReservedEvent.getProductId())
				.quantity(productReservedEvent.getQuantity())
				.userId(productReservedEvent.getUserId())
				.reason(reason)
				.build();
		
		commandGateway.send(publishProductReservationCommand);
	}
	
}
