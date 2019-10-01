/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.impl;

import java.util.function.Supplier;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.util.common.impl.Closer;

public class RoutingKeyBridgeRoutingKeyProvider<E> implements RoutingKeyProvider<E> {

	private final BeanHolder<? extends RoutingKeyBridge> bridgeHolder;

	public RoutingKeyBridgeRoutingKeyProvider(BeanHolder<? extends RoutingKeyBridge> bridgeHolder) {
		this.bridgeHolder = bridgeHolder;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( holder -> holder.get().close(), bridgeHolder );
			closer.push( BeanHolder::close, bridgeHolder );
		}
	}

	@Override
	public String toRoutingKey(Object identifier, Supplier<E> entitySupplier,
			AbstractPojoBackendSessionContext context) {
		return bridgeHolder.get().toRoutingKey(
				context.getTenantIdentifier(), identifier,
				entitySupplier.get(),
				context.getRoutingKeyBridgeToRoutingKeyContext()
		);
	}
}
