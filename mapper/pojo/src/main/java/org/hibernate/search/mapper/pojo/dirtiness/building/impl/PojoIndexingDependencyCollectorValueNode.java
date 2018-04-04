/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAssociationPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * A node representing a value in a dependency collector.
 * <p>
 * The role of dependency collectors is to receive the dependencies (paths to values used during indexing)
 * as an input, and use this information to contribute to
 * {@link org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolver}s.
 * This involves in particular:
 * <ul>
 *     <li>Resolving the path from one entity to another, "contained" entity</li>
 *     <li>Resolving the potential concrete type of "containing" entities</li>
 *     <li>Resolving the potential concrete type of "contained" entities</li>
 *     <li>Inverting the path from one entity to another using a {@link PojoAssociationPathInverter}
 *     <li>Applying the inverted path to the reindexing resolver builder of the "contained" entity,
 *     so that whenever the "contained" entity is modified, the "containing" entity is reindexed.
 *     </li>
 * </ul>
 *
 * @see PojoIndexingDependencyCollectorTypeNode
 *
 * @param <P> The property type
 * @param <V> The extracted value type
 */
public class PojoIndexingDependencyCollectorValueNode<P, V> extends AbstractPojoIndexingDependencyCollectorNode {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoIndexingDependencyCollectorPropertyNode<?, P> parent;
	private final BoundPojoModelPathValueNode<?, P, V> modelPath;

	// First key: inverse side entity type, second key: original side concrete entity type
	private Map<PojoRawTypeModel<?>, Map<PojoRawTypeModel<?>, PojoAssociationPath>> inverseAssociationPathCache =
			new HashMap<>();

	PojoIndexingDependencyCollectorValueNode(PojoIndexingDependencyCollectorPropertyNode<?, P> parent,
			BoundPojoModelPathValueNode<?, P, V> modelPath,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.parent = parent;
		this.modelPath = modelPath;
	}

	public PojoIndexingDependencyCollectorTypeNode<V> type() {
		return new PojoIndexingDependencyCollectorTypeNode<>(
				this, modelPath.type(), buildingHelper
		);
	}

	public void collectDependency() {
		// TODO pass the path to the value somehow, so as to support fine-grained dirty checking
		parent.getParent().collectDependency();
	}

	PojoTypeModel<V> getTypeModel() {
		return modelPath.type().getTypeModel();
	}

	/**
	 * @param typeNodeBuilder A type node builder representing the type of this value as viewed from the contained side.
	 * Its type must be a subtype of the raw type of this value.
	 */
	void markForReindexing(AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> typeNodeBuilder) {
		PojoTypeModel<?> inverseSideEntityType = typeNodeBuilder.getTypeModel();
		PojoRawTypeModel<?> inverseSideRawEntityType = inverseSideEntityType.getRawType();
		PojoTypeModel<V> expectedInverseSideEntityType = modelPath.type().getTypeModel();
		PojoRawTypeModel<?> expectedInverseSideEntityRawType = expectedInverseSideEntityType.getRawType();
		if ( !inverseSideRawEntityType.isSubTypeOf( expectedInverseSideEntityRawType ) ) {
			throw new AssertionFailure(
					"Error while building the automatic reindexing resolver at path " + modelPath
							+ ": the dependency collector was passed a resolver builder with incorrect type; "
							+ " got " + inverseSideRawEntityType + ", but a subtype of " + expectedInverseSideEntityRawType
							+ " was expected."
							+ " This is very probably a bug in Hibernate Search, please report it."
			);
		}

		Map<PojoRawTypeModel<?>, PojoAssociationPath> inverseAssociationsPaths =
				getInverseAssociationPathByConcreteEntityType( typeNodeBuilder.getTypeModel() );
		for ( Map.Entry<PojoRawTypeModel<?>, PojoAssociationPath> entry : inverseAssociationsPaths.entrySet() ) {
			markForReindexingWithOriginalSideConcreteType( entry.getKey(), typeNodeBuilder, entry.getValue() );
		}
	}

	private Map<PojoRawTypeModel<?>, PojoAssociationPath> getInverseAssociationPathByConcreteEntityType(
			PojoTypeModel<?> inverseSideEntityType) {
		PojoRawTypeModel<?> inverseSideRawEntityType = inverseSideEntityType.getRawType();

		// Cache the inverse association path, as we may compute it many times, which may be costly
		Map<PojoRawTypeModel<?>, PojoAssociationPath> result = inverseAssociationPathCache.get( inverseSideRawEntityType );
		if ( result == null ) {
			if ( !inverseAssociationPathCache.containsKey( inverseSideRawEntityType ) ) {
				BoundPojoModelPathPropertyNode<?, ? extends P> originalSidePropertyPath = modelPath.parent();
				PojoTypeModel<?> originalSideEntityType = originalSidePropertyPath.parent().getTypeModel();
				PojoRawTypeModel<?> originalSideRawEntityType = originalSideEntityType.getRawType();
				PojoPropertyModel<?> originalSideProperty = originalSidePropertyPath.getPropertyModel();
				BoundContainerValueExtractorPath<?, ?> originalSideExtractorPath = modelPath.getBoundExtractorPath();

				result = new HashMap<>();

				for ( PojoRawTypeModel<?> concreteEntityType :
						buildingHelper.getConcreteEntitySubTypesForEntitySuperType( originalSideRawEntityType ) ) {
					PojoAssociationPath inverseAssociationPath = buildingHelper.getPathInverter()
							.invertPropertyAndExtractors(
									inverseSideEntityType, concreteEntityType,
									originalSideProperty, originalSideExtractorPath
							)
							.orElse( null );
					if ( inverseAssociationPath == null ) {
						PojoAssociationPath associationPath = new PojoAssociationPath(
								originalSideProperty.getName(), originalSideExtractorPath.getExtractorPath()
						);
						inverseAssociationPathCache.put( inverseSideRawEntityType, null );
						throw log.cannotInvertAssociation(
								inverseSideRawEntityType, concreteEntityType, associationPath
						);
					}

					result.put( concreteEntityType, inverseAssociationPath );
				}

				inverseAssociationPathCache.put( inverseSideRawEntityType, result );
			}
			else {
				// Only report the first error, then ignore silently
				result = Collections.emptyMap();
			}
		}

		return result;
	}

	private void markForReindexingWithOriginalSideConcreteType(PojoTypeModel<?> originalSideConcreteEntityType,
			AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> typeNodeBuilder,
			PojoAssociationPath inverseAssociationPath) {
		PojoTypeModel<?> inverseSideEntityType = typeNodeBuilder.getTypeModel();
		PojoRawTypeModel<?> inverseSideRawEntityType = inverseSideEntityType.getRawType();

		PojoRawTypeModel<?> originalSideRawConcreteEntityType = originalSideConcreteEntityType.getRawType();

		// Attempt to apply the inverse association path to the given builder
		PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> valueNodeBuilderDelegate;
		Set<? extends PojoRawTypeModel<?>> valueNodeTypeConcreteEntitySubTypes;
		try {
			String inversePropertyName = inverseAssociationPath.getPropertyName();
			ContainerValueExtractorPath inverseExtractorPath = inverseAssociationPath.getExtractorPath();
			valueNodeBuilderDelegate = typeNodeBuilder.property( inversePropertyName ).value( inverseExtractorPath );

			/*
			 * The entities to reindex will always be instances of both the entity type on the original side
			 * (because that's the one we want to reindex)
			 * and the type targeted by the inverse side of the association
			 * (because that's all we will ever retrieve at runtime).
			 * Thus we will only consider the most specific type of the two when resolving entities to reindex.
			 */
			PojoRawTypeModel<?> valueNodeRawType = valueNodeBuilderDelegate.getTypeModel().getRawType();
			if ( valueNodeRawType.isSubTypeOf( originalSideRawConcreteEntityType ) ) {
				valueNodeTypeConcreteEntitySubTypes =
						buildingHelper.getConcreteEntitySubTypesForEntitySuperType( valueNodeRawType );
			}
			else if ( originalSideRawConcreteEntityType.isSubTypeOf( valueNodeRawType ) ) {
				valueNodeTypeConcreteEntitySubTypes =
						buildingHelper.getConcreteEntitySubTypesForEntitySuperType( originalSideRawConcreteEntityType );
			}
			else {
				throw log.incorrectTargetTypeForInverseAssociation( valueNodeRawType, originalSideRawConcreteEntityType );
			}
		}
		// Note: this should catch errors related to properties not found, among others.
		catch (RuntimeException e) {
			BoundContainerValueExtractorPath<?, ?> originalSideExtractorPath = modelPath.getBoundExtractorPath();
			PojoAssociationPath associationPath = new PojoAssociationPath(
					modelPath.parent().getPropertyModel().getName(), originalSideExtractorPath.getExtractorPath()
			);
			throw log.cannotApplyInvertAssociationPath(
					inverseSideRawEntityType, inverseAssociationPath,
					originalSideRawConcreteEntityType, associationPath,
					e.getMessage(), e
			);
		}

		// Recurse if necessary
		PojoIndexingDependencyCollectorValueNode<?, ?> entityNodeParentValueNode = parent.getParent().getParent();
		if ( entityNodeParentValueNode != null ) {
			/*
			 * We did not reach the indexed type yet.
			 * Continue to build the inverse path from the "potentially dirty" value to the indexed type.
			 */
			for ( PojoRawTypeModel<?> concreteEntityType : valueNodeTypeConcreteEntitySubTypes ) {
				AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> inverseValueTypeBuilder =
						valueNodeBuilderDelegate.type( concreteEntityType );
				entityNodeParentValueNode.markForReindexing( inverseValueTypeBuilder );
			}
		}
		else {
			/*
			 * We fully built the inverse path from the "potentially dirty" value to the indexed type.
			 * Mark the values at the end of that inverse path as requiring reindexing
			 * when the entity holding the inverse path is dirty.
			 */
			for ( PojoRawTypeModel<?> concreteEntityType : valueNodeTypeConcreteEntitySubTypes ) {
				AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> inverseValueTypeBuilder =
						valueNodeBuilderDelegate.type( concreteEntityType );
				inverseValueTypeBuilder.markForReindexing();
			}
		}
	}

}
