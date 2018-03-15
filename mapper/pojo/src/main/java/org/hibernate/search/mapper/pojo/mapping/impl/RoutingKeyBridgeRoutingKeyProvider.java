/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.model.impl.PojoElementImpl;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;

public class RoutingKeyBridgeRoutingKeyProvider<E> implements RoutingKeyProvider<E> {

	private final RoutingKeyBridge bridge;

	public RoutingKeyBridgeRoutingKeyProvider(RoutingKeyBridge bridge) {
		this.bridge = bridge;
	}

	@Override
	public void close() {
		bridge.close();
	}

	@Override
	public String toRoutingKey(String tenantIdentifier, Object identifier, Supplier<E> entitySupplier) {
		return bridge.toRoutingKey( tenantIdentifier, identifier, new PojoElementImpl( entitySupplier.get() ) );
	}
}
