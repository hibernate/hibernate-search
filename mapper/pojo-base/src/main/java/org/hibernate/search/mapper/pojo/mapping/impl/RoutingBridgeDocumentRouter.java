/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.route.DocumentRoute;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;
import org.hibernate.search.mapper.pojo.route.impl.DocumentRouteImpl;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkRouter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class RoutingBridgeDocumentRouter<E> implements DocumentRoutes, PojoWorkRouter {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final RoutingBridgeRouteContext context;
	private final RoutingBridge<E> routingBridge;
	private final Object entityIdentifier;
	private final E entity;

	private DocumentRouteImpl currentRoute = null;
	private boolean skip = false;

	public RoutingBridgeDocumentRouter(RoutingBridgeRouteContext context, RoutingBridge<E> routingBridge,
			Object entityIdentifier, E entity) {
		this.context = context;
		this.routingBridge = routingBridge;
		this.entityIdentifier = entityIdentifier;
		this.entity = entity;
	}

	@Override
	public DocumentRoute addRoute() {
		if ( currentRoute != null ) {
			// TODO HSEARCH-3971 allow routing to multiple indexes *simultaneously*?
			throw log.multipleCurrentRoutes( routingBridge );
		}
		currentRoute = new DocumentRouteImpl();
		return currentRoute;
	}

	@Override
	public void notIndexed() {
		skip = true;
	}

	@Override
	public DocumentRouteImpl currentRoute(String providedRoutingKey) {
		routingBridge.route( this, entityIdentifier, entity, context );

		if ( skip ) {
			return null;
		}
		if ( currentRoute == null ) {
			throw log.noCurrentRoute( routingBridge );
		}
		if ( providedRoutingKey != null ) {
			currentRoute.routingKey( providedRoutingKey );
		}
		return currentRoute;
	}

	@Override
	public List<DocumentRouteImpl> previousRoutes(DocumentRouteImpl currentRoute) {
		return new PreviousDocumentRoutes().previousDifferentRoutes( currentRoute );
	}

	private final class PreviousDocumentRoutes implements DocumentRoutes {
		private List<DocumentRouteImpl> previousRoutes = null;
		private boolean skip = false;

		@Override
		public DocumentRoute addRoute() {
			if ( previousRoutes == null ) {
				previousRoutes = new ArrayList<>();
			}
			DocumentRouteImpl route = new DocumentRouteImpl();
			previousRoutes.add( route );
			return route;
		}

		@Override
		public void notIndexed() {
			skip = true;
		}

		List<DocumentRouteImpl> previousDifferentRoutes(DocumentRouteImpl currentRoute) {
			routingBridge.previousRoutes( this, entityIdentifier, entity, context );

			if ( skip ) {
				return Collections.emptyList();
			}
			if ( previousRoutes == null || previousRoutes.isEmpty() ) {
				throw log.noPreviousRoute( routingBridge );
			}
			if ( currentRoute != null ) {
				// Remove previous routes that are the same as the current one
				previousRoutes.remove( currentRoute );
			}
			return previousRoutes;
		}
	}
}
