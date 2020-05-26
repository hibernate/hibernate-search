/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;

class AlwaysNullRoutingKeyProvider implements RoutingKeyProvider<Object> {

	static final AlwaysNullRoutingKeyProvider INSTANCE = new AlwaysNullRoutingKeyProvider();

	private AlwaysNullRoutingKeyProvider() {
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public String toRoutingKey(Object identifier, Supplier<Object> entitySupplier, BridgeSessionContext context) {
		return null;
	}
}
