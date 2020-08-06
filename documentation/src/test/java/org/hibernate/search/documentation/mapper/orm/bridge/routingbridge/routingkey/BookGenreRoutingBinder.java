/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.routingbridge.routingkey;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;

//tag::binder[]
public class BookGenreRoutingBinder implements RoutingBinder { // <1>

	@Override
	public void bind(RoutingBindingContext context) { // <2>
		context.dependencies() // <3>
				.use( "genre" );

		context.bridge( // <4>
				Book.class, // <5>
				new Bridge() // <6>
		);
	}

	// ... class continues below
	//end::binder[]
	//tag::bridge[]
	// ... class BookGenreRoutingBinder (continued)

	public static class Bridge implements RoutingBridge<Book> { // <1>
		@Override
		public void route(DocumentRoutes routes, Object entityIdentifier, Book indexedEntity, // <2>
				RoutingBridgeRouteContext context) {
			String routingKey = indexedEntity.getGenre().name(); // <3>
			routes.addRoute().routingKey( routingKey ); // <4>
		}

		@Override
		public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, Book indexedEntity, // <5>
				RoutingBridgeRouteContext context) {
			for ( Genre possiblePreviousGenre : Genre.values() ) {
				String routingKey = possiblePreviousGenre.name();
				routes.addRoute().routingKey( routingKey ); // <6>
			}
		}
	}
}
//end::bridge[]
