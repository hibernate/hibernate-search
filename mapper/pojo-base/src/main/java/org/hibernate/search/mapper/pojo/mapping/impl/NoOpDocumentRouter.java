/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.mapper.pojo.route.impl.DocumentRouteImpl;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkRouter;

class NoOpDocumentRouter implements PojoWorkRouter {

	static final NoOpDocumentRouter INSTANCE = new NoOpDocumentRouter();

	@Override
	public DocumentRouteImpl currentRoute(String providedRoutingKey) {
		DocumentRouteImpl route = new DocumentRouteImpl();
		if ( providedRoutingKey != null ) {
			route.routingKey( providedRoutingKey );
		}
		return route;
	}

	@Override
	public List<DocumentRouteImpl> previousRoutes(DocumentRouteImpl currentRoute) {
		return Collections.emptyList();
	}
}
