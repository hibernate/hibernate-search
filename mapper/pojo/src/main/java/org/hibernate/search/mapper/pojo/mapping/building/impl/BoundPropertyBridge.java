/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelPropertyRootElement;

public final class BoundPropertyBridge<P> {
	private final PropertyBridge bridge;
	private final PojoModelPropertyRootElement<P> pojoModelRootElement;

	BoundPropertyBridge(PropertyBridge bridge, PojoModelPropertyRootElement<P> pojoModelRootElement) {
		this.bridge = bridge;
		this.pojoModelRootElement = pojoModelRootElement;
	}

	public PropertyBridge getBridge() {
		return bridge;
	}

	public PojoModelPropertyRootElement<P> getPojoModelRootElement() {
		return pojoModelRootElement;
	}
}
