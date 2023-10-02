/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.dependency.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorDisjointValueNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.dependency.PojoOtherEntityIndexingDependencyConfigurationContext;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoModelPathBinder;

class PojoOtherEntityIndexingDependencyConfigurationContextImpl<T>
		implements
		PojoOtherEntityIndexingDependencyConfigurationContext {
	private final BoundPojoModelPath.Walker bindingPathWalker;
	private final BoundPojoModelPathTypeNode<T> modelPath;
	private final BoundPojoModelPathValueNode<?, ?, ?> boundPathFromOtherEntityTypeToBridgedType;
	private final List<BoundPojoModelPathValueNode<?, ?, ?>> usedPaths = new ArrayList<>();

	PojoOtherEntityIndexingDependencyConfigurationContextImpl(BoundPojoModelPath.Walker bindingPathWalker,
			BoundPojoModelPathTypeNode<T> modelPath,
			BoundPojoModelPathValueNode<?, ?, ?> boundPathFromOtherEntityTypeToBridgedType) {
		this.bindingPathWalker = bindingPathWalker;
		this.modelPath = modelPath;
		this.boundPathFromOtherEntityTypeToBridgedType = boundPathFromOtherEntityTypeToBridgedType;
	}

	@Override
	public PojoOtherEntityIndexingDependencyConfigurationContext use(PojoModelPathValueNode pathFromBridgedTypeToUsedValue) {
		BoundPojoModelPathValueNode<?, ?, ?> boundPath = PojoModelPathBinder.bind(
				modelPath, pathFromBridgedTypeToUsedValue, bindingPathWalker
		);
		usedPaths.add( boundPath );
		return this;
	}

	void contributeDependencies(PojoIndexingDependencyCollectorTypeNode<?> dependencyCollector) {
		PojoIndexingDependencyCollectorDisjointValueNode<?> dependencyCollectorDisjointValueNode =
				dependencyCollector.disjointValue( boundPathFromOtherEntityTypeToBridgedType );

		PojoIndexingDependencyCollectorTypeNode<?> dependencyCollectorTypeNode =
				dependencyCollectorDisjointValueNode.type();

		for ( BoundPojoModelPathValueNode<?, ?, ?> usedPath : usedPaths ) {
			PojoModelPathBinder.bind(
					dependencyCollectorTypeNode,
					usedPath.toUnboundPath(),
					PojoIndexingDependencyCollectorNode.walker()
			);
		}
	}
}
