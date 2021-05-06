/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.route;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.util.common.impl.Contracts;

public final class DocumentRoutesDescriptor implements Serializable {

	public static DocumentRoutesDescriptor of(DocumentRouteDescriptor currentRoute) {
		return of( currentRoute, Collections.emptySet() );
	}

	public static DocumentRoutesDescriptor of(DocumentRouteDescriptor currentRoute,
			Collection<DocumentRouteDescriptor> previousRoutes) {
		return new DocumentRoutesDescriptor( currentRoute, previousRoutes );
	}

	/**
	 * A util to convert the legacy representation of a route (a single string) to a {@link DocumentRoutesDescriptor}.
	 * <p>
	 * It will assume no routes are given if {@code providedRoutingKey} is {@code null},
	 * so it's not possible to represent the default route using this util.
	 *
	 * @param providedRoutingKey The provided routing key, or {@code null}.
	 * @return The corresponding routes, or {@code null}.
	 */
	public static DocumentRoutesDescriptor fromLegacyRoutingKey(String providedRoutingKey) {
		if ( providedRoutingKey == null ) {
			return null;
		}
		return of( DocumentRouteDescriptor.of( providedRoutingKey ) );
	}

	private final DocumentRouteDescriptor currentRoute;
	private final Collection<DocumentRouteDescriptor> previousRoutes;

	public DocumentRoutesDescriptor(DocumentRouteDescriptor currentRoute,
			Collection<DocumentRouteDescriptor> previousRoutes) {
		this.currentRoute = currentRoute;
		Contracts.assertNotNull( previousRoutes, "previousRoutes" );
		this.previousRoutes = previousRoutes;
	}

	public DocumentRouteDescriptor currentRoute() {
		return currentRoute;
	}

	public Collection<DocumentRouteDescriptor> previousRoutes() {
		return previousRoutes;
	}
}
