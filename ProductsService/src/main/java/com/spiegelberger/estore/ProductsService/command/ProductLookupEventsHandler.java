package com.spiegelberger.estore.ProductsService.command;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ResetHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.spiegelberger.estore.ProductsService.core.data.ProductLookupEntity;
import com.spiegelberger.estore.ProductsService.core.data.ProductLookupRepository;
import com.spiegelberger.estore.ProductsService.core.events.ProductCreatedEvent;

@Component
@ProcessingGroup("product-group")
public class ProductLookupEventsHandler {
	
	private final ProductLookupRepository productLookupRepository;	
	
	@Autowired
	public ProductLookupEventsHandler(ProductLookupRepository productLookupRepository) {
		this.productLookupRepository = productLookupRepository;
	}

	@EventHandler
	public void on(ProductCreatedEvent event) {
		
		ProductLookupEntity productLookupEntity = new ProductLookupEntity(
					event.getProductId(), event.getTitle());
		
		productLookupRepository.save(productLookupEntity);
	}
	
	@ResetHandler
	public void reset() {
		
		productLookupRepository.deleteAll();
	}

}
