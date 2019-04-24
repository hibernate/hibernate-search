/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.model.dependency.PojoTypeDependencyContext;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoTypeDependencyContextImpl;

public class RoutingKeyBridgeBindingContextImpl implements RoutingKeyBridgeBindingContext {
	private final PojoModelType bridgedElement;
	private final PojoTypeDependencyContextImpl pojoDependencyContext;

	public RoutingKeyBridgeBindingContextImpl(PojoModelType bridgedElement,
			PojoTypeDependencyContextImpl pojoDependencyContext) {
		this.bridgedElement = bridgedElement;
		this.pojoDependencyContext = pojoDependencyContext;
	}

	@Override
	public PojoModelType getBridgedElement() {
		return bridgedElement;
	}

	@Override
	public PojoTypeDependencyContext getDependencies() {
		return pojoDependencyContext;
	}
}
