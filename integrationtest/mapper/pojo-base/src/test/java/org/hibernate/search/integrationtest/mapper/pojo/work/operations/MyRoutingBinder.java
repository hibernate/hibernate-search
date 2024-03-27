/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;

public final class MyRoutingBinder implements RoutingBinder {
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public void bind(RoutingBindingContext context) {
		context.dependencies().use( "value" );
		context.bridge( IndexedEntity.class, new MyRoutingBridge() );
	}
}
