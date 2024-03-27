/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoTypeIndexingDependencyConfigurationContextImpl;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;

public final class BoundTypeBridge<T> {
	private final BeanHolder<? extends TypeBridge<? super T>> bridgeHolder;
	private final PojoModelTypeRootElement<T> pojoModelRootElement;
	private final PojoTypeIndexingDependencyConfigurationContextImpl<T> pojoDependencyContext;

	BoundTypeBridge(BeanHolder<? extends TypeBridge<? super T>> bridgeHolder,
			PojoModelTypeRootElement<T> pojoModelRootElement,
			PojoTypeIndexingDependencyConfigurationContextImpl<T> pojoDependencyContext) {
		this.bridgeHolder = bridgeHolder;
		this.pojoModelRootElement = pojoModelRootElement;
		this.pojoDependencyContext = pojoDependencyContext;
	}

	public BeanHolder<? extends TypeBridge<? super T>> getBridgeHolder() {
		return bridgeHolder;
	}

	public TypeBridge<? super T> getBridge() {
		return bridgeHolder.get();
	}

	public void contributeDependencies(PojoIndexingDependencyCollectorTypeNode<T> dependencyCollector) {
		pojoModelRootElement.contributeDependencies( dependencyCollector );
		pojoDependencyContext.contributeDependencies( dependencyCollector );
	}
}
