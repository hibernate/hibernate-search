/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.util.common.impl.Closer;

public class RoutingKeyBridgeRoutingBridgeAdapter<E> implements RoutingBridge<E> {

	private final BeanHolder<? extends RoutingKeyBridge> bridgeHolder;

	public RoutingKeyBridgeRoutingBridgeAdapter(BeanHolder<? extends RoutingKeyBridge> bridgeHolder) {
		this.bridgeHolder = bridgeHolder;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "bridgeHolder=" + bridgeHolder
				+ "]";
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( holder -> holder.get().close(), bridgeHolder );
			closer.push( BeanHolder::close, bridgeHolder );
		}
	}

	@Override
	public void route(DocumentRoutes routes, Object entityIdentifier, E indexedEntity, RoutingBridgeRouteContext context) {
		String routingKey = bridgeHolder.get().toRoutingKey( context.tenantIdentifier(), entityIdentifier,
				indexedEntity, (RoutingKeyBridgeToRoutingKeyContext) context );
		routes.addRoute().routingKey( routingKey );
	}
}
