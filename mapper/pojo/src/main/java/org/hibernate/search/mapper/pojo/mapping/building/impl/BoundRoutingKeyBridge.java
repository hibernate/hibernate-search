/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;

public final class BoundRoutingKeyBridge<T> {
	private final BeanHolder<? extends RoutingKeyBridge> bridgeHolder;
	private final PojoModelTypeRootElement<T> pojoModelRootElement;

	BoundRoutingKeyBridge(BeanHolder<? extends RoutingKeyBridge> bridgeHolder, PojoModelTypeRootElement<T> pojoModelRootElement) {
		this.bridgeHolder = bridgeHolder;
		this.pojoModelRootElement = pojoModelRootElement;
	}

	public BeanHolder<? extends RoutingKeyBridge> getBridgeHolder() {
		return bridgeHolder;
	}

	public PojoModelTypeRootElement<T> getPojoModelRootElement() {
		return pojoModelRootElement;
	}
}
