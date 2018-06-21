/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import org.hibernate.search.mapper.pojo.dirtiness.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;

/**
 * A node representing a property in a dependency collector.
 *
 * @see PojoIndexingDependencyCollectorValueNode
 *
 * @param <P> The property type
 */
public class PojoIndexingDependencyCollectorPropertyNode<T, P> extends AbstractPojoIndexingDependencyCollectorNode {

	private final PojoIndexingDependencyCollectorTypeNode<T> parentNode;
	private final BoundPojoModelPathPropertyNode<T, P> modelPathFromParentNode;
	private final PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode;
	private final BoundPojoModelPathPropertyNode<T, P> modelPathFromLastEntityNode;
	private final BoundPojoModelPathPropertyNode<T, P> modelPathFromRootEntityNode;

	PojoIndexingDependencyCollectorPropertyNode(PojoIndexingDependencyCollectorTypeNode<T> parentNode,
			BoundPojoModelPathPropertyNode<T, P> modelPathFromParentNode,
			PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode,
			BoundPojoModelPathPropertyNode<T, P> modelPathFromLastEntityNode,
			BoundPojoModelPathPropertyNode<T, P> modelPathFromRootEntityNode,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.parentNode = parentNode;
		this.modelPathFromParentNode = modelPathFromParentNode;
		this.lastEntityNode = lastEntityNode;
		this.modelPathFromLastEntityNode = modelPathFromLastEntityNode;
		this.modelPathFromRootEntityNode = modelPathFromRootEntityNode;
	}

	public <V> PojoIndexingDependencyCollectorValueNode<P, V> value(
			BoundContainerValueExtractorPath<P, V> boundExtractorPath) {
		return new PojoIndexingDependencyCollectorValueNode<>(
				this,
				modelPathFromParentNode.value( boundExtractorPath ),
				lastEntityNode,
				modelPathFromLastEntityNode.value( boundExtractorPath ),
				modelPathFromRootEntityNode.value( boundExtractorPath ),
				buildingHelper
		);
	}

	PojoIndexingDependencyCollectorValueNode<P, ?> value(
			ContainerValueExtractorPath extractorPath) {
		BoundContainerValueExtractorPath<P, ?> boundExtractorPath =
				buildingHelper.bindExtractorPath(
						modelPathFromRootEntityNode.getPropertyModel().getTypeModel(),
						extractorPath
				);
		return value( boundExtractorPath );
	}

	@Override
	ReindexOnUpdate getReindexOnUpdate() {
		return parentNode.getReindexOnUpdate();
	}

	PojoIndexingDependencyCollectorTypeNode<T> getParentNode() {
		return parentNode;
	}
}
