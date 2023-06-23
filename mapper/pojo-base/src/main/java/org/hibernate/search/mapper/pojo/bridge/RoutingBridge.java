/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge;

import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
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
	 * <strong>Warning:</strong> Reading from {@code indexedEntity} should be done with care.
	 * Any read that was not declared during {@link RoutingBinder#bind(RoutingBindingContext) binding}
	 * (by declaring dependencies using {@link RoutingBindingContext#dependencies()}
	 * or (advanced use) creating an accessor using {@link RoutingBindingContext#bridgedElement()})
	 * may lead to out-of-sync indexes,
	 * because Hibernate Search will consider the read property irrelevant to indexing
	 * and will not reindex entities when that property changes.
	 * <p>
	 * <strong>Warning:</strong> If the route of a given entity with a given id can change
	 * (e.g. an entity is indexed when its status is {@code PUBLISHED},
	 * but is no longer indexed when it becomes {@code ARCHIVED}),
	 * you <strong>must</strong> also implement {@link #previousRoutes(DocumentRoutes, Object, Object, RoutingBridgeRouteContext)}
	 * to return all the possible previous routes
	 * so that Hibernate Search can clean up previously indexed documents.
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
	 * Defines the potential previous routes for an indexed entity.
	 * <p>
	 * Defining the routes should be done by calling methods on the {@code route} parameter.
	 * Hibernate Search will follow these routes to delete the previous versions of a document.
	 * <p>
	 * Implementing this method is only necessary if the route used to index a document can change,
	 * for example if an entity is indexed when its status is {@code PUBLISHED},
	 * but is no longer indexed when it becomes {@code ARCHIVED}.
	 * If the route never changes, this method can safely delegate to
	 * {@link #route(DocumentRoutes, Object, Object, RoutingBridgeRouteContext)}
	 * to return the same route.
	 * <p>
	 * <strong>Warning:</strong> Reading from {@code indexedEntity} should be done with care.
	 * Any read that was not declared during {@link RoutingBinder#bind(RoutingBindingContext) binding}
	 * (by declaring dependencies using {@link RoutingBindingContext#dependencies()}
	 * or (advanced use) creating an accessor using {@link RoutingBindingContext#bridgedElement()})
	 * may lead to out-of-sync indexes,
	 * because Hibernate Search will consider the read property irrelevant to indexing
	 * and will not reindex entities when that property changes.
	 *
	 * @param routes The routes that were (potentially) used to index the given entity, to be configured by this bridge.
	 * @param entityIdentifier The value of the POJO property used to generate the document identifier,
	 * i.e. the same value that is passed to
	 * {@link IdentifierBridge#toDocumentIdentifier(Object, org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext)}.
	 * @param indexedEntity The indexed entity that should be routed by this bridge.
	 * @param context A context that can be
	 * {@link RoutingBridgeRouteContext#extension(RoutingBridgeRouteContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM Session
	 * (if using the Hibernate ORM mapper).
	 */
	void previousRoutes(DocumentRoutes routes, Object entityIdentifier, E indexedEntity,
			RoutingBridgeRouteContext context);

	/**
	 * Closes any resource before the routing bridge is discarded.
	 */
	@Override
	default void close() {
	}

}
