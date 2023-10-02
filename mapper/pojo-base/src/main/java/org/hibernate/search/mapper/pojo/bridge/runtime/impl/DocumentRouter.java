/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
