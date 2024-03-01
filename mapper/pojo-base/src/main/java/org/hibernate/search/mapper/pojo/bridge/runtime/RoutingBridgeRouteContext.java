/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;
import org.hibernate.search.util.common.SearchException;

/**
 * The context passed to {@link RoutingBridge#route(DocumentRoutes, Object, Object, RoutingBridgeRouteContext)}.
 */
public interface RoutingBridgeRouteContext {

	/**
	 * @return The tenant identifier currently in use ({@code null} if none).
	 */
	String tenantIdentifier();

	/**
	 * @return The tenant identifier currently in use ({@code null} if none).
	 */
	Object tenantIdentifierValue();

	/**
	 * Extend the current context with the given extension,
	 * resulting in an extended context offering more options.
	 *
	 * @param extension The extension to apply.
	 * @param <T> The type of context provided by the extension.
	 * @return The extended context.
	 * @throws SearchException If the extension cannot be applied (wrong underlying technology, ...).
	 */
	<T> T extension(RoutingBridgeRouteContextExtension<T> extension);

}
