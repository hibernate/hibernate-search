/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.routingbridge.conditionalindexing;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;

//tag::binder[]
public class BookStatusRoutingBinder implements RoutingBinder { // <1>

	@Override
	public void bind(RoutingBindingContext context) { // <2>
		context.dependencies() // <3>
				.use( "status" );

		context.bridge( // <4>
				Book.class, // <5>
				new Bridge() // <6>
		);
	}

	// ... class continues below
	//end::binder[]
	//tag::bridge[]
	// ... class BookStatusRoutingBinder (continued)

	public static class Bridge // <1>
			implements RoutingBridge<Book> { // <2>
		@Override
		public void route(DocumentRoutes routes, Object entityIdentifier, // <3>
				Book indexedEntity, RoutingBridgeRouteContext context) {
			switch ( indexedEntity.getStatus() ) { // <4>
				case PUBLISHED:
					routes.addRoute(); // <5>
					break;
				case ARCHIVED:
					routes.notIndexed(); // <6>
					break;
			}
		}

		@Override
		public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, // <7>
				Book indexedEntity, RoutingBridgeRouteContext context) {
			routes.addRoute(); // <8>
		}
	}
}
//end::bridge[]
