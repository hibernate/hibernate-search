/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;

public final class BoundTypeBridge<T> {
	private final BeanHolder<? extends TypeBridge> bridgeHolder;
	private final PojoModelTypeRootElement<T> pojoModelRootElement;

	BoundTypeBridge(BeanHolder<? extends TypeBridge> bridgeHolder, PojoModelTypeRootElement<T> pojoModelRootElement) {
		this.bridgeHolder = bridgeHolder;
		this.pojoModelRootElement = pojoModelRootElement;
	}

	public BeanHolder<? extends TypeBridge> getBridgeHolder() {
		return bridgeHolder;
	}

	public PojoModelTypeRootElement<T> getPojoModelRootElement() {
		return pojoModelRootElement;
	}
}
