/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;

public final class BoundRoutingKeyBridge<T> {
	private final RoutingKeyBridge bridge;
	private final PojoModelTypeRootElement<T> pojoModelRootElement;

	BoundRoutingKeyBridge(RoutingKeyBridge bridge, PojoModelTypeRootElement<T> pojoModelRootElement) {
		this.bridge = bridge;
		this.pojoModelRootElement = pojoModelRootElement;
	}

	public RoutingKeyBridge getBridge() {
		return bridge;
	}

	public PojoModelTypeRootElement<T> getPojoModelRootElement() {
		return pojoModelRootElement;
	}
}
