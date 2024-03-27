/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import java.util.List;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;

public final class MyRoutingBridge implements RoutingBridge<IndexedEntity> {
	protected static boolean indexed = false;
	protected static boolean previouslyIndexed = false;
	protected static List<String> previousValues = null;

	public static String toRoutingKey(Object tenantIdentifier, Object entityIdentifier, String value) {
		StringBuilder keyBuilder = new StringBuilder();
		if ( tenantIdentifier != null ) {
			keyBuilder.append( tenantIdentifier ).append( "/" );
		}
		keyBuilder.append( entityIdentifier ).append( "/" );
		keyBuilder.append( value );
		return keyBuilder.toString();
	}

	@Override
	public void route(DocumentRoutes routes, Object entityIdentifier, IndexedEntity indexedEntity,
			RoutingBridgeRouteContext context) {
		if ( !indexed ) {
			routes.notIndexed();
			return;
		}
		String tenantIdentifier = context.tenantIdentifier();
		routes.addRoute()
				.routingKey( toRoutingKey( tenantIdentifier, entityIdentifier, indexedEntity.value ) );
	}

	@Override
	public void previousRoutes(DocumentRoutes routes, Object entityIdentifier,
			IndexedEntity indexedEntity,
			RoutingBridgeRouteContext context) {
		if ( !previouslyIndexed ) {
			routes.notIndexed();
			return;
		}
		String tenantIdentifier = context.tenantIdentifier();
		if ( previousValues == null ) {
			routes.addRoute()
					.routingKey( toRoutingKey( tenantIdentifier, entityIdentifier, indexedEntity.value ) );
		}
		else {
			for ( String previousValue : previousValues ) {
				routes.addRoute()
						.routingKey( toRoutingKey( tenantIdentifier, entityIdentifier, previousValue ) );
			}
		}
	}
}
