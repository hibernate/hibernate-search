/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.model.PojoModelType;

public class RoutingKeyBridgeBindingContextImpl implements RoutingKeyBridgeBindingContext {
	private final PojoModelType bridgedElement;

	public RoutingKeyBridgeBindingContextImpl(PojoModelType bridgedElement) {
		this.bridgedElement = bridgedElement;
	}

	@Override
	public PojoModelType getBridgedElement() {
		return bridgedElement;
	}
}
