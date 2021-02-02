/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;

public final class NoOpDocumentRouter implements DocumentRouter<Object> {

	public static final NoOpDocumentRouter INSTANCE = new NoOpDocumentRouter();

	private static final DocumentRouteDescriptor DEFAULT_ROUTE = DocumentRouteDescriptor.of( null );
	private static final DocumentRoutesDescriptor DEFAULT_ROUTES = DocumentRoutesDescriptor.of( DEFAULT_ROUTE );

	private NoOpDocumentRouter() {
	}

	@Override
	public DocumentRouteDescriptor currentRoute(Object entityIdentifier, Supplier<?> entitySupplier,
			DocumentRoutesDescriptor providedRoutes, BridgeSessionContext context) {
		return providedRoutes != null ? providedRoutes.currentRoute() : DEFAULT_ROUTE;
	}

	@Override
	public DocumentRoutesDescriptor routes(Object entityIdentifier, Supplier<?> entitySupplier,
			DocumentRoutesDescriptor providedRoutes, BridgeSessionContext context) {
		if ( providedRoutes == null ) {
			return DEFAULT_ROUTES;
		}
		// Clean up the previous routes if necessary
		DocumentRouteDescriptor currentRoute = providedRoutes.currentRoute();
		Collection<DocumentRouteDescriptor> previousRoutes = providedRoutes.previousRoutes();
		if ( previousRoutes.contains( currentRoute ) ) {
			previousRoutes = new ArrayList<>( previousRoutes );
			previousRoutes.remove( currentRoute );
			return DocumentRoutesDescriptor.of( currentRoute, previousRoutes );
		}
		else {
			return providedRoutes;
		}
	}

}
