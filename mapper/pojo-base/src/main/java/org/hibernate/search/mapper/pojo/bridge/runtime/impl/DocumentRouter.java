/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;

public interface DocumentRouter<E> extends AutoCloseable {

	@Override
	default void close() {
	}

	DocumentRouteDescriptor currentRoute(Object entityIdentifier, Supplier<? extends E> entitySupplier,
			DocumentRoutesDescriptor providedRoutes,
			BridgeSessionContext context);

	DocumentRoutesDescriptor routes(Object entityIdentifier, Supplier<? extends E> entitySupplier,
			DocumentRoutesDescriptor providedRoutes,
			BridgeSessionContext context);

}
