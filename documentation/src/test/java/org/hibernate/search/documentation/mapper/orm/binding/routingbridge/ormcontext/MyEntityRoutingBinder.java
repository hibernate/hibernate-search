/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.routingbridge.ormcontext;

import org.hibernate.Session;
import org.hibernate.search.mapper.orm.HibernateOrmExtension;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;
import org.hibernate.search.util.common.AssertionFailure;

public class MyEntityRoutingBinder implements RoutingBinder {

	@Override
	public void bind(RoutingBindingContext context) {
		context.dependencies()
				.useRootOnly();

		context.bridge( MyEntity.class, new Bridge() );
	}

	//tag::include[]
	private static class Bridge implements RoutingBridge<MyEntity> {

		@Override
		public void route(DocumentRoutes routes, Object entityIdentifier, MyEntity indexedEntity,
				RoutingBridgeRouteContext context) {
			Session session = context.extension( HibernateOrmExtension.get() ) // <1>
					.session(); // <2>
			// ... do something with the session ...
			//end::include[]
			/*
			 * I don't know what to do with the session here,
			 * so I'm just going to extract data from it and index the entity or not depending on that data.
			 * This is silly, but at least it allows us to check the session was successfully retrieved.
			 */
			MyData dataFromSession = (MyData) session.getProperties().get( "test.data.indexed" );
			switch ( dataFromSession ) {
				case INDEXED:
					routes.addRoute();
					break;
				case NOT_INDEXED:
					routes.notIndexed();
					break;
				default:
					throw new AssertionFailure( "Unexpected data from session: " + dataFromSession );
			}
			//tag::include[]
		}

		@Override
		public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, MyEntity indexedEntity,
				RoutingBridgeRouteContext context) {
			// ...
			//end::include[]
			routes.addRoute();
			//tag::include[]
		}
	}
	//end::include[]
}
