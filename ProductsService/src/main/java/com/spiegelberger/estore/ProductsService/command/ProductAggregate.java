package com.spiegelberger.estore.ProductsService.command;

import java.math.BigDecimal;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import com.spiegelberger.estore.ProductsService.core.events.ProductCreatedEvent;
import com.spiegelberger.estore.core.commands.ReserveProductCommand;
import com.spiegelberger.estore.core.events.ProductReservedEvent;

@Aggregate
public class ProductAggregate {
	
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
		
		BeanUtils.copyProperties(createProductCommand, productCreatedEvent);
		
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
	
	
	@EventSourcingHandler
	public void on(ProductCreatedEvent productCreatedEvent) {
		
		this.productId=productCreatedEvent.getProductId();
		this.title=productCreatedEvent.getTitle();
		this.price=productCreatedEvent.getPrice();
		this.quantity=productCreatedEvent.getQuantity();
	}
	
	@EventSourcingHandler
	public void on(ProductReservedEvent productReservedEvent) {
		this.quantity -= productReservedEvent.getQuantity();
	}
}
