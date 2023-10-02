/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.automaticindexing;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;

public class StatusRoutingBridge implements RoutingBridge<RoutedIndexedEntity> {

	@Override
	public void route(DocumentRoutes routes, Object entityIdentifier, RoutedIndexedEntity indexedEntity,
			RoutingBridgeRouteContext context) {
		routes.addRoute().routingKey( indexedEntity.getStatus().name() );
	}

	@Override
	public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, RoutedIndexedEntity indexedEntity,
			RoutingBridgeRouteContext context) {
		// Let's assume that business rules mandate that status change always happens in the same direction:
		// first => second => third
		// and that we never "skip" a step in a given transaction.
		// Then we know what the previous routes can be.
		switch ( indexedEntity.getStatus() ) {
			case FIRST:
				routes.notIndexed();
				break;
			case SECOND:
				routes.addRoute().routingKey( RoutedIndexedEntity.Status.FIRST.name() );
				break;
			case THIRD:
				routes.addRoute().routingKey( RoutedIndexedEntity.Status.SECOND.name() );
				break;
		}
	}

	public static final class Binder implements RoutingBinder {

		@Override
		public void bind(RoutingBindingContext context) {
			context.dependencies().use( "text" );
			context.bridge( RoutedIndexedEntity.class, new StatusRoutingBridge() );
		}
	}
}
