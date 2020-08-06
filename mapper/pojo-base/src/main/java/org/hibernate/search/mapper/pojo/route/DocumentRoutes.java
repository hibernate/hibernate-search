/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.route;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;

/**
 * The object passed to a {@link RoutingBridge}
 * to define where an entity should be indexed.
 */
public interface DocumentRoutes {

	/**
	 * Add a route that indexing operations for this entity should take,
	 * based on its current state.
	 * <p>
	 * At the moment, only one current route can be added per indexed entity.
	 *
	 * @return A new route, to be defined in more details.
	 */
	DocumentRoute addRoute();

	/**
	 * Instructs Hibernate Search that the entity should not be indexed.
	 * <p>
	 * Calling this method will lead to the {@link #addRoute() route} being ignored.
	 */
	void notIndexed();

}
