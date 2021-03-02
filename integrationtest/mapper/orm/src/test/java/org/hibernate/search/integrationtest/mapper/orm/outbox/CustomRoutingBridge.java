/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.outbox;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;

public class CustomRoutingBridge implements RoutingBridge<RoutedIndexedEntity> {

	public static final int NUMBER_OF_SHARD = 7;

	@Override
	public void route(DocumentRoutes routes, Object entityIdentifier, RoutedIndexedEntity indexedEntity,
			RoutingBridgeRouteContext context) {
		String text = indexedEntity.getText();
		char selector;

		if ( text == null || text.trim().isEmpty() ) {
			selector = 'z';
		}
		else {
			selector = text.charAt( 0 );
		}

		// this is not a really great way to define the route id,
		// we're using here just for testing
		int shardId = selector % NUMBER_OF_SHARD;
		routes.addRoute().routingKey( shardId + "" );
	}

	@Override
	public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, RoutedIndexedEntity indexedEntity,
			RoutingBridgeRouteContext context) {
		for ( int i = 0; i < 7; i++ ) {
			// we simply don’t know what the previous route id of the book was,
			// so we tell Hibernate Search to follow all possible routes
			routes.addRoute().routingKey( i + "" );
		}
	}

	public static final class CustomRoutingBinder implements RoutingBinder {

		@Override
		public void bind(RoutingBindingContext context) {
			context.dependencies().use( "text" );
			context.bridge( RoutedIndexedEntity.class, new CustomRoutingBridge() );
		}
	}
}
