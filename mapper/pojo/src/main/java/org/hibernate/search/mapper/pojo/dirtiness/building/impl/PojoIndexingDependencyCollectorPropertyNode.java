/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

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

	private final BoundPojoModelPathPropertyNode<T, P> modelPathFromRootEntityNode;
	private final PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode;
	private final BoundPojoModelPathPropertyNode<T, P> modelPathFromLastEntityNode;

	PojoIndexingDependencyCollectorPropertyNode(BoundPojoModelPathPropertyNode<T, P> modelPathFromRootEntityNode,
			PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode,
			BoundPojoModelPathPropertyNode<T, P> modelPathFromLastEntityNode,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.modelPathFromRootEntityNode = modelPathFromRootEntityNode;
		this.lastEntityNode = lastEntityNode;
		this.modelPathFromLastEntityNode = modelPathFromLastEntityNode;
	}

	public <V> PojoIndexingDependencyCollectorValueNode<P, V> value(
			BoundContainerValueExtractorPath<P, V> boundExtractorPath) {
		return new PojoIndexingDependencyCollectorValueNode<>(
				modelPathFromRootEntityNode.value( boundExtractorPath ),
				lastEntityNode,
				modelPathFromLastEntityNode.value( boundExtractorPath ),
				buildingHelper
		);
	}

}
