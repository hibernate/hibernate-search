/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import org.hibernate.search.mapper.pojo.dirtiness.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.binding.impl.PojoModelPathWalker;

public abstract class PojoIndexingDependencyCollectorNode {

	public static Walker walker() {
		return new Walker( null );
	}

	static Walker walker(PojoIndexingDependencyCollectorValueNode<?, ?> initialNodeCollectingDependency) {
		return new Walker( initialNodeCollectingDependency );
	}

	final PojoImplicitReindexingResolverBuildingHelper buildingHelper;

	PojoIndexingDependencyCollectorNode(PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		this.buildingHelper = buildingHelper;
	}

	abstract ReindexOnUpdate getReindexOnUpdate();

	static class Walker implements PojoModelPathWalker<
			PojoIndexingDependencyCollectorTypeNode<?>,
			PojoIndexingDependencyCollectorPropertyNode<?, ?>,
			PojoIndexingDependencyCollectorValueNode<?, ?>
			> {
		private final PojoIndexingDependencyCollectorValueNode<?, ?> initialNodeCollectingDependency;

		Walker(PojoIndexingDependencyCollectorValueNode<?, ?> initialNodeCollectingDependency) {
			this.initialNodeCollectingDependency = initialNodeCollectingDependency;
		}

		@Override
		public PojoIndexingDependencyCollectorPropertyNode<?, ?> property(
				PojoIndexingDependencyCollectorTypeNode<?> typeNode, String propertyName) {
			return typeNode.property( propertyName );
		}

		@Override
		public PojoIndexingDependencyCollectorValueNode<?, ?> value(
				PojoIndexingDependencyCollectorPropertyNode<?, ?> propertyNode,
				ContainerExtractorPath extractorPath) {
			PojoIndexingDependencyCollectorValueNode<?, ?> node = propertyNode.value( extractorPath );
			node.doCollectDependency( initialNodeCollectingDependency );
			return node;
		}

		@Override
		public PojoIndexingDependencyCollectorTypeNode<?> type(
				PojoIndexingDependencyCollectorValueNode<?, ?> valueNode) {
			return valueNode.type();
		}
	}
}
