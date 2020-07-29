/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge;

import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContextExtension;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;

/**
 * An object responsible for routing an indexed entity to the correct index/shard.
 */
public interface RoutingBridge<E> extends AutoCloseable {

	/**
	 * Routes an indexed entity.
	 * <p>
	 * Defining the route should be done by calling methods on the {@code route} parameter.
	 * <p>
	 * <strong>Warning:</strong> The route of a given entity with a given id
	 * <strong>must remain the same until the entity is deleted</strong>.
	 * Changing the route for a given entity after an update will lead to duplicate documents
	 * in the index.
	 * For that reason, it is recommended that any access to {@code indexedEntity}
	 * relies exclusively on effectively immutable properties,
	 * i.e. properties that won't change after the creation of the entity.
	 *
	 * @param routes The routes to follow when indexing the given entity, to be configured by this bridge.
	 * @param entityIdentifier The value of the POJO property used to generate the document identifier,
	 * i.e. the same value that is passed to
	 * {@link IdentifierBridge#toDocumentIdentifier(Object, org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext)}.
	 * @param indexedEntity The indexed entity that should be routed by this bridge.
	 * @param context A context that can be
 	 * {@link RoutingBridgeRouteContext#extension(RoutingBridgeRouteContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM Session
	 * (if using the Hibernate ORM mapper).
	 */
	void route(DocumentRoutes routes, Object entityIdentifier, E indexedEntity, RoutingBridgeRouteContext context);

	/**
	 * Closes any resource before the routing bridge is discarded.
	 */
	@Override
	default void close() {
	}

}
