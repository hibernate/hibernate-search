/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;

abstract class AbstractPojoIndexingDependencyCollectorValueNode extends PojoIndexingDependencyCollectorNode {

	AbstractPojoIndexingDependencyCollectorValueNode(
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
	}

	public abstract PojoIndexingDependencyCollectorTypeNode<?> type();

	abstract void collectDependency(BoundPojoModelPathValueNode<?, ?, ?> dirtyPathFromEntityType);

	/**
	 * Make sure that whenever the value represented by {@code dependencyPathFromInverseSideEntityTypeNode} changes
	 * in the entity at the root of {@code inverseSideEntityTypeNodeBuilder},
	 * we trigger reindexing of the entity at the root of this dependency collector.
	 *
	 * @param inverseSideEntityTypeNodeBuilder A type node builder representing the type of this value as viewed from the contained side.
	 * Its type must be a subtype of the raw type of this value.
	 * Its type must be an {@link PojoTypeAdditionalMetadata#isEntity() entity type}.
	 * @param dependencyPathFromInverseSideEntityTypeNode The path from the given entity type node
	 * to the property being used when reindexing.
	 */
	abstract void markForReindexing(
			AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> inverseSideEntityTypeNodeBuilder,
			BoundPojoModelPathValueNode<?, ?, ?> dependencyPathFromInverseSideEntityTypeNode);

}
