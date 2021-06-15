package com.spiegelberger.estore.ProductsService.query;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.spiegelberger.estore.ProductsService.core.data.ProductEntity;
import com.spiegelberger.estore.ProductsService.core.data.ProductsRepository;
import com.spiegelberger.estore.ProductsService.core.events.ProductCreatedEvent;

@Component
@ProcessingGroup("product-group")
public class ProductEventsHandler {
	
	private ProductsRepository productsRepository;
	
	@Autowired
	public ProductEventsHandler(ProductsRepository productsRepository) {
		this.productsRepository = productsRepository;
	}



	@EventHandler
	public void on(ProductCreatedEvent event) {
		
		ProductEntity productEntity = new ProductEntity();
		BeanUtils.copyProperties(event, productEntity);
		
		productsRepository.save(productEntity);
	}
}
