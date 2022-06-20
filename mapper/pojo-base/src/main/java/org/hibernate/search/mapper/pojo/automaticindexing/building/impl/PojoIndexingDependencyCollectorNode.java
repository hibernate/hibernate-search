/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.binding.impl.PojoModelPathWalker;
import org.hibernate.search.util.common.data.impl.LinkedNode;

public abstract class PojoIndexingDependencyCollectorNode {

	public static Walker walker() {
		return new Walker( null );
	}

	static Walker walker(LinkedNode<DerivedDependencyWalkingInfo> derivedDependencyPath) {
		return new Walker( derivedDependencyPath );
	}

	final PojoImplicitReindexingResolverBuildingHelper buildingHelper;

	PojoIndexingDependencyCollectorNode(PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		this.buildingHelper = buildingHelper;
	}

	ReindexOnUpdate composeReindexOnUpdate(PojoIndexingDependencyCollectorTypeNode<?> otherEntityNode,
			ReindexOnUpdate otherReindexOnUpdate) {
		if ( otherReindexOnUpdate == null ) {
			otherReindexOnUpdate = buildingHelper.getDefaultReindexOnUpdate();
		}
		// Whatever reindexOnUpdate is strictest wins.
		ReindexOnUpdate strictestReindexOnUpdate = getStrictestReindexOnUpdate( reindexOnUpdate(), otherReindexOnUpdate );
		if ( ReindexOnUpdate.SHALLOW.equals( strictestReindexOnUpdate )
				&& !lastEntityNode().equals( otherEntityNode ) ) {
			// We crossed entity boundaries: SHALLOW becomes NO.
			return ReindexOnUpdate.NO;
		}
		return strictestReindexOnUpdate;
	}

	private ReindexOnUpdate getStrictestReindexOnUpdate(ReindexOnUpdate left, ReindexOnUpdate right) {
		// From the least strict to most strict: DEFAULT, SHALLOW, NO.
		// That's also the ordinal order, so we just usse that.
		return left.ordinal() < right.ordinal() ? right : left;
	}

	/**
	 * The last entity node among the ancestor nodes (this node included).
	 * The "last entity node" might be the same as the last type node
	 * if the last type node represents an entity type.
	 * If not (e.g. if the last type node represents an embeddable type),
	 * then the "last entity node" will be the closest ancestor type node representing an entity type.
	 */
	abstract PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode();

	abstract ReindexOnUpdate reindexOnUpdate();

	static class Walker implements PojoModelPathWalker<
			PojoIndexingDependencyCollectorTypeNode<?>,
			PojoIndexingDependencyCollectorPropertyNode<?, ?>,
			AbstractPojoIndexingDependencyCollectorDirectValueNode<?, ?>
			> {
		private final LinkedNode<DerivedDependencyWalkingInfo> derivedDependencyPath;

		Walker(LinkedNode<DerivedDependencyWalkingInfo> derivedDependencyPath) {
			this.derivedDependencyPath = derivedDependencyPath;
		}

		@Override
		public PojoIndexingDependencyCollectorPropertyNode<?, ?> property(
				PojoIndexingDependencyCollectorTypeNode<?> typeNode, String propertyName) {
			return typeNode.property( propertyName );
		}

		@Override
		public AbstractPojoIndexingDependencyCollectorDirectValueNode<?, ?> value(
				PojoIndexingDependencyCollectorPropertyNode<?, ?> propertyNode,
				ContainerExtractorPath extractorPath) {
			AbstractPojoIndexingDependencyCollectorDirectValueNode<?, ?> node = propertyNode.value( extractorPath );
			node.doCollectDependency( derivedDependencyPath );
			return node;
		}

		@Override
		public PojoIndexingDependencyCollectorTypeNode<?> type(
				AbstractPojoIndexingDependencyCollectorDirectValueNode<?, ?> valueNode) {
			return valueNode.type();
		}
	}
}
