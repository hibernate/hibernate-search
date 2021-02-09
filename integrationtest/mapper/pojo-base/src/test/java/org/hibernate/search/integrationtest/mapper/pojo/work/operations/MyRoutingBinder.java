/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
