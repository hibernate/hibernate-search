/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.util.common.data.impl.LinkedNode;

/**
 * A node representing a value in a dependency collector,
 * and considering that the type holding the property/value
 * has the different metadata from its subtypes.
 *
 * @param <P> The property type
 * @param <V> The extracted value type
 *
 * @see AbstractPojoIndexingDependencyCollectorDirectValueNode
 * @see PojoIndexingDependencyCollectorTypeNode
 */
public class PojoIndexingDependencyCollectorPolymorphicDirectValueNode<P, V>
		extends AbstractPojoIndexingDependencyCollectorDirectValueNode<P, V> {

	static <P, V> AbstractPojoIndexingDependencyCollectorDirectValueNode<P, V> create(
			PojoIndexingDependencyCollectorPropertyNode<?, P> parentNode,
			BoundPojoModelPathValueNode<?, P, V> modelPathFromLastEntityNode,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		List<? extends PojoIndexingDependencyCollectorTypeNode<?>> holderSubTypeNodes =
				parentNode.parentNode().polymorphic();
		String propertyName = parentNode.modelPathFromParentNode().getPropertyModel().name();
		BoundContainerExtractorPath<? super P, V> boundExtractorPath = modelPathFromLastEntityNode.getBoundExtractorPath();
		List<PojoIndexingDependencyCollectorMonomorphicDirectValueNode<? extends P, V>> monomorphicValueNodes =
				new ArrayList<>();
		Metadata parentTypeMetadata = Metadata.create(
				buildingHelper, parentNode, boundExtractorPath.getExtractorPath() );
		boolean hasDifferentMetadata = false;
		for ( PojoIndexingDependencyCollectorTypeNode<?> holderSubTypeNode : holderSubTypeNodes ) {
			// We're working on a subtype, so the same property always has the same type or a more precise type.
			@SuppressWarnings("unchecked")
			PojoIndexingDependencyCollectorPropertyNode<?, ? extends P> propertyNode =
					(PojoIndexingDependencyCollectorPropertyNode<?, ? extends P>) holderSubTypeNode.property(
							propertyName );
			PojoIndexingDependencyCollectorMonomorphicDirectValueNode<? extends P, V> valueNode = propertyNode
					.monomorphicValue( boundExtractorPath );
			monomorphicValueNodes.add( valueNode );
			hasDifferentMetadata = hasDifferentMetadata || !parentTypeMetadata.equals( valueNode.metadata );
		}
		if ( hasDifferentMetadata ) {
			// Some values are handled differently depending on the holder subtype.
			return new PojoIndexingDependencyCollectorPolymorphicDirectValueNode<>( parentNode,
					modelPathFromLastEntityNode, parentTypeMetadata, monomorphicValueNodes, buildingHelper
			);
		}
		else {
			// No need to use polymorphism; just return the value as it is on the (super) holder type.
			return new PojoIndexingDependencyCollectorMonomorphicDirectValueNode<>( parentNode,
					modelPathFromLastEntityNode, parentTypeMetadata, buildingHelper
			);
		}
	}

	private final List<PojoIndexingDependencyCollectorMonomorphicDirectValueNode<? extends P, V>> monomorphicValueNodes;

	PojoIndexingDependencyCollectorPolymorphicDirectValueNode(
			PojoIndexingDependencyCollectorPropertyNode<?, P> parentNode,
			BoundPojoModelPathValueNode<?, P, V> modelPathFromLastEntityNode,
			Metadata metadata,
			List<PojoIndexingDependencyCollectorMonomorphicDirectValueNode<? extends P, V>> monomorphicValueNodes,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( parentNode, modelPathFromLastEntityNode, metadata, buildingHelper );
		this.monomorphicValueNodes = monomorphicValueNodes;
	}

	@Override
	public void collectDependency() {
		for ( PojoIndexingDependencyCollectorMonomorphicDirectValueNode<?, ?> node : monomorphicValueNodes ) {
			node.collectDependency();
		}
	}

	@Override
	void collectDependency(BoundPojoModelPathValueNode<?, ?, ?> dirtyPathFromEntityType) {
		for ( PojoIndexingDependencyCollectorMonomorphicDirectValueNode<?, ?> node : monomorphicValueNodes ) {
			node.collectDependency( dirtyPathFromEntityType );
		}
	}

	@Override
	void doCollectDependency(LinkedNode<DerivedDependencyWalkingInfo> derivedDependencyPath) {
		for ( PojoIndexingDependencyCollectorMonomorphicDirectValueNode<?, ?> node : monomorphicValueNodes ) {
			node.doCollectDependency( derivedDependencyPath );
		}
	}

	@Override
	void markForReindexing(AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> inverseSideEntityTypeNodeBuilder,
			BoundPojoModelPathValueNode<?, ?, ?> dependencyPathFromInverseSideEntityTypeNode) {
		for ( PojoIndexingDependencyCollectorMonomorphicDirectValueNode<?, ?> node : monomorphicValueNodes ) {
			node.markForReindexing(
					inverseSideEntityTypeNodeBuilder,
					dependencyPathFromInverseSideEntityTypeNode
			);
		}
	}

}
