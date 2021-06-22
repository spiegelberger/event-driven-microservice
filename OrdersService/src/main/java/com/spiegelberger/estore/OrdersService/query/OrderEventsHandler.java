/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spiegelberger.estore.OrdersService.query;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.spiegelberger.estore.OrdersService.core.data.OrderEntity;
import com.spiegelberger.estore.OrdersService.core.data.OrdersRepository;
import com.spiegelberger.estore.OrdersService.core.events.OrderApprovedEvent;
import com.spiegelberger.estore.OrdersService.core.events.OrderCreatedEvent;
import com.spiegelberger.estore.OrdersService.core.events.OrderRejectedEvent;

@Component
@ProcessingGroup("order-group")
public class OrderEventsHandler {
    
    private final OrdersRepository ordersRepository;
    
    public OrderEventsHandler(OrdersRepository ordersRepository) {
        this.ordersRepository = ordersRepository;
    }

    @EventHandler
    public void on(OrderCreatedEvent event) throws Exception {
        OrderEntity orderEntity = new OrderEntity();
        BeanUtils.copyProperties(event, orderEntity);
 
        this.ordersRepository.save(orderEntity);
    }
    
    @EventHandler
    public void on(OrderApprovedEvent orderApprovedEvent) {
    	OrderEntity orderEntity =
    			ordersRepository.findByOrderId(orderApprovedEvent.getOrderId());
    		if(orderEntity == null) {
    			return;
    		}
    		
    	orderEntity.setOrderStatus(orderApprovedEvent.getOrderStatus());
    	
    	ordersRepository.save(orderEntity);
    }
    
    
    @EventHandler
    public void on(OrderRejectedEvent orderRejectedEvent) {
    	
    	OrderEntity orderEntity = ordersRepository.findByOrderId(orderRejectedEvent.getOrderId());
    	
    	orderEntity.setOrderStatus(orderRejectedEvent.getOrderStatus()); 
    	
    	ordersRepository.save(orderEntity);
    }
    
    
    
}
