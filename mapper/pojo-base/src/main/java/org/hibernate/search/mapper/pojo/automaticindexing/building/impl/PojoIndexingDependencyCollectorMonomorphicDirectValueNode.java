/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.binding.impl.PojoModelPathBinder;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.util.common.data.impl.LinkedNode;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A node representing a value in a dependency collector,
 * and considering that the type holding the property/value
 * has the same metadata from its subtypes.
 *
 * @param <P> The property type
 * @param <V> The extracted value type
 *
 * @see AbstractPojoIndexingDependencyCollectorDirectValueNode
 * @see PojoIndexingDependencyCollectorTypeNode
 */
public class PojoIndexingDependencyCollectorMonomorphicDirectValueNode<P, V>
		extends AbstractPojoIndexingDependencyCollectorDirectValueNode<P, V> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	static <P, V> PojoIndexingDependencyCollectorMonomorphicDirectValueNode<P, V> create(
			PojoIndexingDependencyCollectorPropertyNode<?, P> parentNode,
			BoundPojoModelPathValueNode<?, P, V> modelPathFromLastEntityNode,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		return new PojoIndexingDependencyCollectorMonomorphicDirectValueNode<>( parentNode,
				modelPathFromLastEntityNode,
				Metadata.create( buildingHelper, parentNode, modelPathFromLastEntityNode.getExtractorPath() ),
				buildingHelper
		);
	}

	PojoIndexingDependencyCollectorMonomorphicDirectValueNode(
			PojoIndexingDependencyCollectorPropertyNode<?, P> parentNode,
			BoundPojoModelPathValueNode<?, P, V> modelPathFromLastEntityNode,
			Metadata metadata,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( parentNode, modelPathFromLastEntityNode, metadata, buildingHelper );
	}

	@Override
	public PojoIndexingDependencyCollectorTypeNode<V> type() {
		return new PojoIndexingDependencyCollectorTypeNode<>(
				this,
				modelPathFromLastEntityNode.type(),
				buildingHelper
		);
	}

	@Override
	public void collectDependency() {
		doCollectDependency( null );
	}

	@Override
	void collectDependency(BoundPojoModelPathValueNode<?, ?, ?> dirtyPathFromEntityType) {
		if ( metadata.derivedFrom.isEmpty() ) {
			parentNode.parentNode().collectDependency( dirtyPathFromEntityType );
		}
		else {
			// This value is derived from other properties.
			// Any part of this value is assumed to be derived from the same properties:
			// we don't care about which part in particular.
			collectDependency();
		}
	}

	@Override
	void doCollectDependency(LinkedNode<DerivedDependencyWalkingInfo> derivedDependencyPath) {
		ReindexOnUpdate composedReindexOnUpdate = derivedDependencyPath == null ? metadata.reindexOnUpdate
				: derivedDependencyPath.last.value.node.composeReindexOnUpdate( lastEntityNode(), metadata.reindexOnUpdate );
		if ( ReindexOnUpdate.NO.equals( composedReindexOnUpdate ) ) {
			// Updates are ignored
			return;
		}

		if ( metadata.derivedFrom.isEmpty() ) {
			parentNode.parentNode().collectDependency( this.modelPathFromLastEntityNode );
		}
		else {
			/*
			 * The value represented by this node is derived from other, base values.
			 * If we rely on the value represented by this node when indexing,
			 * then we indirectly rely on these base values.
			 *
			 * We don't just call lastEntityNode.collectDependency() for each path to the base values,
			 * because the paths may cross the entity boundaries, meaning they may have a prefix
			 * leading to a different entity, and a suffix leading to the value we rely on.
			 * This means we must go through the dependency collector tree to properly resolve
			 * the entities that should trigger reindexing of our root entity when they change.
			 */
			PojoIndexingDependencyCollectorTypeNode<?> lastTypeNode = parentNode.parentNode();
			for ( PojoModelPathValueNode path : metadata.derivedFrom ) {
				DerivedDependencyWalkingInfo newDerivedDependencyInfo = new DerivedDependencyWalkingInfo( this, path );
				if ( derivedDependencyPath != null ) {
					checkForDerivedDependencyCycle( derivedDependencyPath, newDerivedDependencyInfo );
				}
				LinkedNode<DerivedDependencyWalkingInfo> updatedDerivedDependencyPath =
						derivedDependencyPath == null ? LinkedNode.of( newDerivedDependencyInfo )
								: derivedDependencyPath.withHead( newDerivedDependencyInfo );
				PojoModelPathBinder.bind(
						lastTypeNode, path,
						PojoIndexingDependencyCollectorNode.walker( updatedDerivedDependencyPath )
				);
			}
		}
	}

	private void checkForDerivedDependencyCycle(LinkedNode<DerivedDependencyWalkingInfo> derivedDependencyPath,
			DerivedDependencyWalkingInfo newDerivedDependencyInfo) {
		Optional<LinkedNode<DerivedDependencyWalkingInfo>> cycle = derivedDependencyPath.findAndReverse(
				other -> newDerivedDependencyInfo.definingTypeModel.equals( other.definingTypeModel )
						&& newDerivedDependencyInfo.derivedFromPath.equals( other.derivedFromPath ) );
		if ( cycle.isPresent() ) {
			/*
			 * We found a cycle in the derived dependency path.
			 * This can happen for example if:
			 * - property "foo" on type A is marked as derived from itself
			 * - property "foo" on type A is marked as derived from property "bar" on type B,
			 *   which is marked as derived from property "foo" on type "A".
			 * - property "foo" on type A is marked as derived from property "bar" on type B,
			 *   which is marked as derived from property "foobar" on type "C".
			 *   which is marked as derived from property "bar" on type "B".
			 * Even if such a dependency might work in practice at runtime,
			 * for example because the link A => B never leads to a B that refers to the same A,
			 * even indirectly,
			 * we cannot support it here because we need to model dependencies as a static tree,
			 * which in such case would have an infinite depth.
			 */
			throw log.infiniteRecursionForDerivedFrom( newDerivedDependencyInfo.definingTypeModel,
					cycle.get() );
		}
	}

}
