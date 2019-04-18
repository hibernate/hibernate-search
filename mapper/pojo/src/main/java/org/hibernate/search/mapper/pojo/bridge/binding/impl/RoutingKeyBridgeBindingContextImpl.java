/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.model.dependency.PojoDependencyContext;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoTypeDependencyContext;

public class RoutingKeyBridgeBindingContextImpl implements RoutingKeyBridgeBindingContext {
	private final PojoModelType bridgedElement;
	private final PojoTypeDependencyContext<?> pojoDependencyContext;

	public RoutingKeyBridgeBindingContextImpl(PojoModelType bridgedElement,
			PojoTypeDependencyContext<?> pojoDependencyContext) {
		this.bridgedElement = bridgedElement;
		this.pojoDependencyContext = pojoDependencyContext;
	}

	@Override
	public PojoModelType getBridgedElement() {
		return bridgedElement;
	}

	@Override
	public PojoDependencyContext getDependencies() {
		return pojoDependencyContext;
	}
}
