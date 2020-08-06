/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.RoutingKeyBridgeRoutingBridgeAdapter;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoTypeIndexingDependencyConfigurationContextImpl;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;

@SuppressWarnings("deprecation")
public final class BoundRoutingKeyBridge<T> extends BoundRoutingBridge<T> {
	private final PojoModelTypeRootElement<T> pojoModelRootElement;
	private final PojoTypeIndexingDependencyConfigurationContextImpl<T> pojoDependencyContext;

	BoundRoutingKeyBridge(BeanHolder<? extends org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge> bridgeHolder,
			PojoModelTypeRootElement<T> pojoModelRootElement,
			PojoTypeIndexingDependencyConfigurationContextImpl<T> pojoDependencyContext) {
		super( BeanHolder.of( new RoutingKeyBridgeRoutingBridgeAdapter<>( bridgeHolder ) ),
				pojoModelRootElement, null );
		this.pojoModelRootElement = pojoModelRootElement;
		this.pojoDependencyContext = pojoDependencyContext;
	}

	@Override
	public void contributeDependencies(PojoIndexingDependencyCollectorTypeNode<T> dependencyCollector) {
		pojoModelRootElement.contributeDependencies( dependencyCollector );
		pojoDependencyContext.contributeDependencies( dependencyCollector );
	}
}
