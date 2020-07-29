/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.route.DocumentRoute;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.route.impl.DocumentRouteImpl;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class DocumentRoutesImpl<E> implements DocumentRoutes {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final RoutingBridge<E> routingBridge;
	private final Object entityIdentifier;
	private final E entity;

	private DocumentRouteImpl route = null;

	public DocumentRoutesImpl(RoutingBridge<E> routingBridge, Object entityIdentifier, E entity) {
		this.routingBridge = routingBridge;
		this.entityIdentifier = entityIdentifier;
		this.entity = entity;
	}

	@Override
	public DocumentRoute addRoute() {
		if ( route != null ) {
			// TODO HSEARCH-3971 allow routing to multiple indexes *simultaneously*?
			throw log.multipleIndexingRoutes( routingBridge );
		}
		route = new DocumentRouteImpl();
		return route;
	}

	public String toRoutingKey(RoutingBridgeRouteContext context) {
		routingBridge.route( this, entityIdentifier, entity, context );

		if ( route == null ) {
			throw log.noIndexingRoute( routingBridge );
		}

		return route.routingKey();
	}
}
