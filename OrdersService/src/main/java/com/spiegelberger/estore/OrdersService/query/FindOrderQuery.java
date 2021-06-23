package com.spiegelberger.estore.OrdersService.query;

import lombok.Value;


@Value
public class FindOrderQuery {

	private final String orderId;
}
