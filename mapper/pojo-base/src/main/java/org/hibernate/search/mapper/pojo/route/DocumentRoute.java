/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.route;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;

/**
 * A route for an indexed document, i.e. the object used by a {@link RoutingBridge}
 * to define where an entity should be indexed.
 */
public interface DocumentRoute {

	/**
	 * Sets the routing key, i.e. the key used to select the correct shard in the targeted index.
	 * @param routingKey The routing key. Never {@code null}.
	 */
	void routingKey(String routingKey);

}
