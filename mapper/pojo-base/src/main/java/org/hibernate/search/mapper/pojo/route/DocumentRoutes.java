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
	 * Instructs Hibernate Search to index the entity using a specific route.
	 * <p>
	 * At the moment, only one route can be added per indexed entity.
	 *
	 * @return A new route, to be defined in more details.
	 */
	DocumentRoute addRoute();

	/**
	 * Instructs Hibernate Search not to index the entity.
	 * <p>
	 * Used to exclude a subset of entities from being indexed.
	 * <p><strong>Warning:</strong> Calling this will not cause existing document to be removed!
	 * This method should only be used to exclude entities from being indexed during their
	 * <strong>entire lifespan</strong>, from their creation to their deletion.
	 * <p>
	 * Calling this method will lead to the {@link #addRoute() route} being ignored.
	 */
	void notIndexed();

}
