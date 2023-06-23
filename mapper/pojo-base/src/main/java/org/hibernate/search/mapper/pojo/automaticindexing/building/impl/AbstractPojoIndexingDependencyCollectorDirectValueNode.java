/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoModelPathBinder;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.data.impl.LinkedNode;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A node representing a value in a dependency collector.
 * <p>
 * The role of dependency collectors is to receive the dependencies (paths to values used during indexing)
 * as an input, and use this information to contribute to
 * {@link PojoImplicitReindexingResolverNode}s.
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
 * @param <P> The property type
 * @param <V> The extracted value type
 *
 * @see PojoIndexingDependencyCollectorTypeNode
 */
public abstract class AbstractPojoIndexingDependencyCollectorDirectValueNode<P, V>
		extends AbstractPojoIndexingDependencyCollectorValueNode {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	final PojoIndexingDependencyCollectorPropertyNode<?, P> parentNode;
	final BoundPojoModelPathValueNode<?, P, V> modelPathFromLastEntityNode;

	final Metadata metadata;

	// First key: inverse side entity type, second key: original side concrete entity type
	private final Map<PojoRawTypeModel<?>, Map<PojoRawTypeModel<?>, PojoModelPathValueNode>> inverseAssociationPathCache =
			new HashMap<>();

	AbstractPojoIndexingDependencyCollectorDirectValueNode(PojoIndexingDependencyCollectorPropertyNode<?, P> parentNode,
			BoundPojoModelPathValueNode<?, P, V> modelPathFromLastEntityNode,
			Metadata metadata,
			PojoImplicitReindexingResolverBuildingHelper buildingHelper) {
		super( buildingHelper );
		this.parentNode = parentNode;
		this.modelPathFromLastEntityNode = modelPathFromLastEntityNode;
		this.metadata = metadata;
	}

	@Override
	public PojoIndexingDependencyCollectorTypeNode<V> type() {
		return new PojoIndexingDependencyCollectorTypeNode<>(
				this,
				modelPathFromLastEntityNode.type(),
				buildingHelper
		);
	}

	public <U> PojoIndexingDependencyCollectorTypeNode<? extends U> castedType(PojoRawTypeModel<U> typeModel) {
		return new PojoIndexingDependencyCollectorTypeNode<>(
				this,
				modelPathFromLastEntityNode.type().castTo( typeModel ),
				buildingHelper
		);
	}

	public abstract void collectDependency();

	abstract void doCollectDependency(LinkedNode<DerivedDependencyWalkingInfo> derivedDependencyPath);

	@Override
	final PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode() {
		return parentNode.lastEntityNode();
	}

	@Override
	final ReindexOnUpdate reindexOnUpdate() {
		return metadata.reindexOnUpdate;
	}

	@Override
	abstract void collectDependency(BoundPojoModelPathValueNode<?, ?, ?> dirtyPathFromEntityType);

	@Override
	void markForReindexing(AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> inverseSideEntityTypeNodeBuilder,
			BoundPojoModelPathValueNode<?, ?, ?> dependencyPathFromInverseSideEntityTypeNode) {
		PojoTypeModel<?> inverseSideEntityType = inverseSideEntityTypeNodeBuilder.getTypeModel();
		PojoRawTypeModel<?> inverseSideRawEntityType = inverseSideEntityType.rawType();
		PojoTypeModel<V> expectedInverseSideEntityType = modelPathFromLastEntityNode.type().getTypeModel();
		PojoRawTypeModel<?> expectedInverseSideEntityRawType = expectedInverseSideEntityType.rawType();
		if ( !inverseSideRawEntityType.isSubTypeOf( expectedInverseSideEntityRawType ) ) {
			throw new AssertionFailure(
					"Error while building the automatic reindexing resolver at path " + modelPathFromLastEntityNode
							+ ": the dependency collector was passed a resolver builder with incorrect type; "
							+ " got " + inverseSideRawEntityType + ", but a subtype of " + expectedInverseSideEntityRawType
							+ " was expected."
			);
		}

		Map<PojoRawTypeModel<?>, PojoModelPathValueNode> inverseAssociationsPaths =
				getInverseAssociationPathByConcreteEntityType( inverseSideEntityTypeNodeBuilder.getTypeModel() );
		for ( Map.Entry<PojoRawTypeModel<?>, PojoModelPathValueNode> entry : inverseAssociationsPaths.entrySet() ) {
			markForReindexingUsingAssociationInverseSideWithOriginalSideConcreteType(
					entry.getKey(), inverseSideEntityTypeNodeBuilder, entry.getValue(),
					dependencyPathFromInverseSideEntityTypeNode
			);
		}
	}

	private Map<PojoRawTypeModel<?>, PojoModelPathValueNode> getInverseAssociationPathByConcreteEntityType(
			PojoTypeModel<?> inverseSideEntityType) {
		PojoRawTypeModel<?> inverseSideRawEntityType = inverseSideEntityType.rawType();

		// Cache the inverse association path, as we may compute it many times, which may be costly
		Map<PojoRawTypeModel<?>, PojoModelPathValueNode> result = inverseAssociationPathCache.get(
				inverseSideRawEntityType );
		if ( result == null ) {
			if ( !inverseAssociationPathCache.containsKey( inverseSideRawEntityType ) ) {
				PojoTypeModel<?> originalSideEntityType = lastEntityNode().typeModel();
				PojoRawTypeModel<?> originalSideRawEntityType = originalSideEntityType.rawType();

				// Use a LinkedHashMap for deterministic iteration
				result = new LinkedHashMap<>();

				for ( PojoRawTypeModel<?> concreteEntityType : buildingHelper
						.getConcreteEntitySubTypesForEntitySuperType( originalSideRawEntityType ) ) {
					BoundPojoModelPathValueNode<?, ?, ?> modelPathFromConcreteEntitySubType =
							applyProcessingPathToSubType( concreteEntityType, modelPathFromLastEntityNode );
					PojoModelPathValueNode inverseAssociationPath = buildingHelper.pathInverter()
							.invertPath( inverseSideEntityType, modelPathFromConcreteEntitySubType )
							.orElse( null );
					if ( inverseAssociationPath == null ) {
						throw log.cannotInvertAssociationForReindexing(
								inverseSideRawEntityType, concreteEntityType,
								modelPathFromLastEntityNode.toUnboundPath()
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

	private void markForReindexingUsingAssociationInverseSideWithOriginalSideConcreteType(
			PojoTypeModel<?> originalSideConcreteEntityType,
			AbstractPojoImplicitReindexingResolverTypeNodeBuilder<?, ?> typeNodeBuilder,
			PojoModelPathValueNode inverseAssociationPath,
			BoundPojoModelPathValueNode<?, ?, ?> dependencyPathFromInverseSideEntityTypeNode) {
		PojoTypeModel<?> inverseSideEntityType = typeNodeBuilder.getTypeModel();
		PojoRawTypeModel<?> inverseSideRawEntityType = inverseSideEntityType.rawType();

		PojoRawTypeModel<?> originalSideRawConcreteEntityType = originalSideConcreteEntityType.rawType();

		// Attempt to apply the inverse association path to the given builder
		PojoImplicitReindexingResolverValueNodeBuilderDelegate<?> valueNodeBuilderDelegate;
		Set<? extends PojoRawTypeModel<?>> valueNodeTypeConcreteEntitySubTypes;
		try {
			valueNodeBuilderDelegate = PojoModelPathBinder.bind(
					typeNodeBuilder, inverseAssociationPath, PojoImplicitReindexingResolverBuilder.walker()
			);

			PojoRawTypeModel<?> inverseSideRawType = valueNodeBuilderDelegate.getTypeModel().rawType();
			valueNodeTypeConcreteEntitySubTypes = lastEntityNode().getConcreteEntitySubTypesForTypeToReindex(
					originalSideRawConcreteEntityType, inverseSideRawType
			);
		}
		// Note: this should catch errors related to properties not found, among others.
		catch (RuntimeException e) {
			throw log.cannotApplyImplicitInverseAssociationPath(
					inverseSideRawEntityType, inverseAssociationPath,
					originalSideRawConcreteEntityType, modelPathFromLastEntityNode.toUnboundPath(),
					e.getMessage(), e
			);
		}

		lastEntityNode().markForReindexing(
				valueNodeBuilderDelegate,
				valueNodeTypeConcreteEntitySubTypes,
				dependencyPathFromInverseSideEntityTypeNode
		);
	}

	private BoundPojoModelPathValueNode<?, ?, ?> applyProcessingPathToSubType(PojoRawTypeModel<?> rootSubType,
			BoundPojoModelPathValueNode<?, ?, ?> source) {
		return PojoModelPathBinder.bind(
				BoundPojoModelPath.root( rootSubType ),
				source.toUnboundPath(),
				BoundPojoModelPath.walker( buildingHelper.extractorBinder() )
		);
	}

	protected static final class Metadata {
		static Metadata create(PojoImplicitReindexingResolverBuildingHelper buildingHelper,
				PojoIndexingDependencyCollectorPropertyNode<?, ?> parentNode,
				ContainerExtractorPath containerExtractorPath) {
			PojoTypeModel<?> holderType = parentNode.parentNode().typeModel();
			String propertyName = parentNode.modelPathFromParentNode().getPropertyModel().name();

			ReindexOnUpdate metadataReindexOnUpdateOrNull = buildingHelper.getMetadataReindexOnUpdateOrNull(
					holderType, propertyName, containerExtractorPath );

			PojoIndexingDependencyCollectorTypeNode<?> lastEntityNode = parentNode.lastEntityNode();
			ReindexOnUpdate reindexOnUpdate =
					parentNode.composeReindexOnUpdate( lastEntityNode, metadataReindexOnUpdateOrNull );

			Set<PojoModelPathValueNode> derivedFrom = buildingHelper.getMetadataDerivedFrom(
					holderType, propertyName, containerExtractorPath );

			return new Metadata( reindexOnUpdate, derivedFrom );
		}

		final ReindexOnUpdate reindexOnUpdate;
		final Set<PojoModelPathValueNode> derivedFrom;

		private Metadata(ReindexOnUpdate reindexOnUpdate, Set<PojoModelPathValueNode> derivedFrom) {
			this.derivedFrom = derivedFrom;
			this.reindexOnUpdate = reindexOnUpdate;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Metadata metadata = (Metadata) o;
			return reindexOnUpdate == metadata.reindexOnUpdate && Objects.equals( derivedFrom, metadata.derivedFrom );
		}

		@Override
		public int hashCode() {
			return Objects.hash( reindexOnUpdate, derivedFrom );
		}
	}
}
