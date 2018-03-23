/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.spi.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAssociationPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
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

	private Optional<PojoAssociationPath> inverseAssociationPathOptional;

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
	 */
	void markForReindexing(PojoImplicitReindexingResolverTypeNodeBuilder<?> typeNodeBuilder) {
		BoundPojoModelPathPropertyNode<?, ? extends P> originalSidePropertyPath = modelPath.parent();
		PojoTypeModel<?> originalSideEntityType = originalSidePropertyPath.parent().getTypeModel();
		PojoRawTypeModel<?> originalSideRawEntityType = originalSideEntityType.getRawType();

		// Cache the inverse association path, as we may compute it many times, which may be costly
		if ( inverseAssociationPathOptional == null ) {
			PojoTypeModel<?> inverseSideEntityType = modelPath.type().getTypeModel();
			PojoPropertyModel<?> originalSideProperty = originalSidePropertyPath.getPropertyModel();
			BoundContainerValueExtractorPath<?, ?> originalSideExtractorPath = modelPath.getBoundExtractorPath();
			inverseAssociationPathOptional =
					buildingHelper.getPathInverter().invertPropertyAndExtractors(
							inverseSideEntityType, originalSideRawEntityType,
							originalSideProperty, originalSideExtractorPath
					);
			if ( !inverseAssociationPathOptional.isPresent() ) {
				PojoAssociationPath associationPath = new PojoAssociationPath(
						originalSideProperty.getName(), originalSideExtractorPath.getExtractorPath()
				);
				throw log.cannotInvertAssociation(
						inverseSideEntityType.getRawType(), originalSideRawEntityType, associationPath
				);
			}
		}
		else if ( !inverseAssociationPathOptional.isPresent() ) {
			// Only report the first error, then ignore silently
			return;
		}

		PojoAssociationPath inverseAssociationPath = inverseAssociationPathOptional.get();

		// Attempt to apply the inverse association path to the given builder
		PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> valueNodeBuilderDelegate;
		PojoImplicitReindexingResolverTypeNodeBuilder<?> inverseValueTypeBuilder;
		try {
			String inversePropertyName = inverseAssociationPath.getPropertyName();
			ContainerValueExtractorPath inverseExtractorPath = inverseAssociationPath.getExtractorPath();
			valueNodeBuilderDelegate = typeNodeBuilder.property( inversePropertyName ).value( inverseExtractorPath );
			inverseValueTypeBuilder = valueNodeBuilderDelegate.type();
			checkInverseValueNode( inverseValueTypeBuilder.getType() );
		}
		// Note: this should catch errors related to properties not found as well as related to an incompatible type
		catch (RuntimeException e) {
			PojoPropertyModel<?> originalSideProperty = originalSidePropertyPath.getPropertyModel();
			BoundContainerValueExtractorPath<?, ?> originalSideExtractorPath = modelPath.getBoundExtractorPath();
			PojoAssociationPath associationPath = new PojoAssociationPath(
					originalSideProperty.getName(), originalSideExtractorPath.getExtractorPath()
			);
			throw log.cannotApplyInvertAssociationPath(
					typeNodeBuilder.getType().getRawType(), inverseAssociationPath,
					originalSideRawEntityType, associationPath,
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
			entityNodeParentValueNode.markForReindexing( inverseValueTypeBuilder );
		}
		else {
			/*
			 * We fully built the inverse path from the "potentially dirty" value to the indexed type.
			 * Mark the values at the end of that inverse path as requiring reindexing
			 * when the entity holding the inverse path is dirty.
			 */
			valueNodeBuilderDelegate.markForReindexing();
		}
	}

	/**
	 * Check that the computed inverse of this association has the expected type,
	 * i.e. a supertype of the entity type on this side of the association.
	 * @param inverseValueType The type of the inverse value found for the current association
	 */
	private void checkInverseValueNode(PojoTypeModel<?> inverseValueType) {
		BoundPojoModelPathPropertyNode<?, ? extends P> originalSidePropertyPath = modelPath.parent();
		BoundPojoModelPathTypeNode<?> originalSideEntityTypePath = originalSidePropertyPath.parent();

		PojoRawTypeModel<?> expectedType = originalSideEntityTypePath.getTypeModel().getRawType();
		PojoRawTypeModel<?> actualType = inverseValueType.getRawType();
		if ( ! expectedType.getRawType().isSubTypeOf( actualType.getRawType() ) ) {
			throw log.incorrectTargetTypeForInverseAssociation( actualType, expectedType );
		}
	}

}
