/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.model.impl.PojoBridgedElement;
import org.hibernate.search.mapper.pojo.bridge.spi.RoutingKeyBridge;

public class RoutingKeyBridgeProvider<E> implements RoutingKeyProvider<E> {

	private final RoutingKeyBridge bridge;

	public RoutingKeyBridgeProvider(RoutingKeyBridge bridge) {
		this.bridge = bridge;
	}

	@Override
	public String toRoutingKey(String tenantIdentifier, Object identifier, Supplier<E> entitySupplier) {
		return bridge.apply( tenantIdentifier, identifier, new PojoBridgedElement( entitySupplier.get() ) );
	}
}
