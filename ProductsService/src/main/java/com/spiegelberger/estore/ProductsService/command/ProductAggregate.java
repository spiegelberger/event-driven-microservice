package com.spiegelberger.estore.ProductsService.command;

import java.math.BigDecimal;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import com.spiegelberger.estore.ProductsService.core.events.ProductCreatedEvent;
import com.spiegelberger.estore.core.commands.CancelProductReservationCommand;
import com.spiegelberger.estore.core.commands.ReserveProductCommand;
import com.spiegelberger.estore.core.events.ProductReservationCancelledEvent;
import com.spiegelberger.estore.core.events.ProductReservedEvent;

// The domain object. Holds the current state of the object

@Aggregate(snapshotTriggerDefinition ="productSnapshotTriggerDefinition")
public class ProductAggregate {
	
	// This field will associate the command and the aggregate
	@AggregateIdentifier
	private String productId;
	
	private String title;
	private BigDecimal price;
	private Integer quantity;

	public ProductAggregate() {
		
	}
	
	@CommandHandler
	public ProductAggregate(CreateProductCommand createProductCommand) {

		//validate createProduct Command
		if(createProductCommand.getPrice().compareTo(BigDecimal.ZERO)<=0) {
			throw new IllegalArgumentException("Price cannot be less or equal to zero.");
		}
		
		if(createProductCommand.getTitle()==null || createProductCommand.getTitle().isBlank()) {
			throw new IllegalArgumentException("Title cannot be empty");
		}
		
		ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent();
		
		// Generally this is solved by using a builder method if command class doesn't have too many fields
		BeanUtils.copyProperties(createProductCommand, productCreatedEvent);
		
		/*
		Aggregate mostly publish events by applying them
		This will dispatch the aggregate to all event handlers inside this aggregate, 
		so that its state could be updated.
		*/
		AggregateLifecycle.apply(productCreatedEvent);
		
	}
	
	
	@CommandHandler
	public void handle(ReserveProductCommand reserveProductCommand) {
		
		if(quantity<reserveProductCommand.getQuantity()) {
			throw new IllegalArgumentException("Insufficent number of items in stock");
		}
		
		ProductReservedEvent productReservedEvent = ProductReservedEvent.builder()
				.orderId(reserveProductCommand.getOrderId())
				.productId(reserveProductCommand.getProductId())
				.quantity(reserveProductCommand.getQuantity())
				.userId(reserveProductCommand.getUserId())
				.build();
		
		AggregateLifecycle.apply(productReservedEvent);
		
	}
	
	
	@CommandHandler
	public void handle(CancelProductReservationCommand cancelProductReservationCommand) {
		
		ProductReservationCancelledEvent  productReservationCancelledEvent =
				 ProductReservationCancelledEvent.builder()
				 .orderId(cancelProductReservationCommand.getOrderId())
				 .productId(cancelProductReservationCommand.getProductId())
				 .quantity(cancelProductReservationCommand.getQuantity())
				 .reason(cancelProductReservationCommand.getReason())
				 .userId(cancelProductReservationCommand.getUserId())
				 .build();
		
		AggregateLifecycle.apply(productReservationCancelledEvent);
				 				 
	}
	
	//Initialize the current state of the aggregate based on the latest information
	@EventSourcingHandler
	public void on(ProductCreatedEvent productCreatedEvent) {
		
		this.productId = productCreatedEvent.getProductId();
		this.price = productCreatedEvent.getPrice();
		this.title = productCreatedEvent.getTitle();
		this.quantity = productCreatedEvent.getQuantity();
		
	}
	
	
	@EventSourcingHandler
	public void on(ProductReservedEvent productReservedEvent) {
		this.quantity -= productReservedEvent.getQuantity();
		
	}
	
	
	@EventSourcingHandler
	public void on(ProductReservationCancelledEvent  productReservationCancelledEvent) {
		
		this.quantity += productReservationCancelledEvent.getQuantity();
		
	}
	
	
	
}
