/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;

/**
 * A node representing a property in a dependency collector.
 *
 * @see AbstractPojoIndexingDependencyCollectorDirectValueNode
 *
 * @param <P> The property type
 */
public class PojoIndexingDependencyCollectorPropertyNode<T, P> extends PojoIndexingDependencyCollectorNode {

	private final PojoIndexingDependencyCollectorTypeNode<T> parentNode;
	/**
	 * The path to this node from the parent node, i.e. from the node representing the type holding this property.
	 */
	private final BoundPojoModelPathPropertyNode<T, P> modelPathFromParentNode;
	private final BoundPojoModelPathPropertyNode<T, P> modelPathFromLastEntityNode;

	PojoIndexingDependencyCollectorPropertyNode(PojoIndexingDependencyCollectorTypeNode<T> parentNode,
			BoundPojoModelPathPropertyNode<T, P> modelPathFromParentNode,
			BoundPojoModelPathPropertyNode<T, P> modelPathFromLastEntityNode,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.parentNode = parentNode;
		this.modelPathFromParentNode = modelPathFromParentNode;
		this.modelPathFromLastEntityNode = modelPathFromLastEntityNode;
	}

	public <V> AbstractPojoIndexingDependencyCollectorDirectValueNode<P, V> value(
			BoundContainerExtractorPath<? super P, V> boundExtractorPath) {
		return PojoIndexingDependencyCollectorPolymorphicDirectValueNode.create(
				this,
				modelPathFromLastEntityNode.value( boundExtractorPath ),
				buildingHelper
		);
	}

	public AbstractPojoIndexingDependencyCollectorDirectValueNode<P, ?> value(
			ContainerExtractorPath extractorPath) {
		BoundContainerExtractorPath<P, ?> boundExtractorPath =
				buildingHelper.extractorBinder().bindPath(
						modelPathFromLastEntityNode.getPropertyModel().typeModel(),
						extractorPath
				);
		return value( boundExtractorPath );
	}

	<V> PojoIndexingDependencyCollectorMonomorphicDirectValueNode<P, V> monomorphicValue(
			BoundContainerExtractorPath<? super P, V> boundExtractorPath) {
		return PojoIndexingDependencyCollectorMonomorphicDirectValueNode.create(
				this,
				modelPathFromLastEntityNode.value( boundExtractorPath ),
				buildingHelper
		);
	}

	@Override
	PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode() {
		return parentNode.lastEntityNode();
	}

	@Override
	ReindexOnUpdate reindexOnUpdate() {
		return parentNode.reindexOnUpdate();
	}

	PojoIndexingDependencyCollectorTypeNode<T> parentNode() {
		return parentNode;
	}

	BoundPojoModelPathPropertyNode<T, P> modelPathFromParentNode() {
		return modelPathFromParentNode;
	}
}
