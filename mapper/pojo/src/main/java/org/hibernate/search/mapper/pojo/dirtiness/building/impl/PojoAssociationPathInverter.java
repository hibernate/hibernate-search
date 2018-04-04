/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerValueExtractorBinder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.model.augmented.building.impl.PojoAugmentedTypeModelProvider;
import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAssociationPath;
import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAugmentedPropertyModel;
import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAugmentedTypeModel;
import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAugmentedValueModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * An object responsible for inverting an association path,
 * i.e. a chain of properties and container value extractors going from one entity to another.
 */
public final class PojoAssociationPathInverter {
	private final PojoAugmentedTypeModelProvider augmentedTypeModelProvider;
	private final PojoBootstrapIntrospector introspector;
	private final ContainerValueExtractorBinder extractorBinder;

	public PojoAssociationPathInverter(PojoAugmentedTypeModelProvider augmentedTypeModelProvider,
			PojoBootstrapIntrospector introspector,
			ContainerValueExtractorBinder extractorBinder) {
		this.augmentedTypeModelProvider = augmentedTypeModelProvider;
		this.introspector = introspector;
		this.extractorBinder = extractorBinder;
	}

	public Optional<PojoAssociationPath> invertPropertyAndExtractors(
			PojoTypeModel<?> inverseSideEntityType, PojoRawTypeModel<?> originalSideEntityType,
			PojoPropertyModel<?> originalSideProperty,
			BoundContainerValueExtractorPath<?, ?> originalSideBoundExtractorPath) {
		List<PojoAssociationPath> associationPathsToMatch = computeAssociationPathsToMatch(
				originalSideProperty, originalSideBoundExtractorPath
		);

		// Try to find inverse side information hosted on the side to inverse
		Optional<PojoAssociationPath> inverseSidePathOptional =
				findInverseSidePathFromOriginalSide(
						originalSideEntityType, associationPathsToMatch
				);

		if ( !inverseSidePathOptional.isPresent() ) {
			// Try to find inverse side information hosted on the other side
			inverseSidePathOptional = findInverseSidePathFromInverseSide(
					inverseSideEntityType, originalSideEntityType, associationPathsToMatch
			);
		}

		return inverseSidePathOptional;
	}

	/*
	 * One might refer to the extractor path of an association in multiple ways:
	 * - By intension, e.g. ContainerValueExtractorPath.default()
	 * - By extension, e.g. ContainerValueExtractorPath.noExtractors()
	 *   or ContainerValueExtractorPath.explicitExtractors( ... )
	 * We want to match any type of reference, so we have to determine whether this association
	 * uses the default extractor path, and if so, add it to the list of paths to match.
	 */
	private List<PojoAssociationPath> computeAssociationPathsToMatch(PojoPropertyModel<?> originalSideProperty,
			BoundContainerValueExtractorPath<?, ?> originalSideBoundExtractorPath) {
		List<PojoAssociationPath> associationPathsToMatch = new ArrayList<>();
		associationPathsToMatch.add( new PojoAssociationPath(
				originalSideProperty.getName(), originalSideBoundExtractorPath.getExtractorPath()
		) );
		if ( isDefaultExtractorPath( originalSideProperty, originalSideBoundExtractorPath ) ) {
			associationPathsToMatch.add( new PojoAssociationPath(
					originalSideProperty.getName(),
					ContainerValueExtractorPath.defaultExtractors()
			) );
		}
		return associationPathsToMatch;
	}

	private boolean isDefaultExtractorPath(PojoPropertyModel<?> propertyModel,
			BoundContainerValueExtractorPath<?, ?> originalSideBoundExtractorPath) {
		Optional<? extends BoundContainerValueExtractorPath<?, ?>> boundDefaultExtractorPathOptional = extractorBinder
				.tryBindPath(
						introspector, propertyModel.getTypeModel(),
						ContainerValueExtractorPath.defaultExtractors()
				);
		return boundDefaultExtractorPathOptional.isPresent() && originalSideBoundExtractorPath.getExtractorPath().equals(
				boundDefaultExtractorPathOptional.get().getExtractorPath()
		);
	}

	private Optional<PojoAssociationPath> findInverseSidePathFromOriginalSide(
			PojoRawTypeModel<?> originalSideEntityType,
			List<PojoAssociationPath> associationPathsToMatch) {
		PojoAugmentedTypeModel augmentedTypeModel =
				augmentedTypeModelProvider.get( originalSideEntityType );
		for ( PojoAssociationPath pathToMatch : associationPathsToMatch ) {
			Optional<PojoAssociationPath> result = augmentedTypeModel.getProperty( pathToMatch.getPropertyName() )
					.getValue( pathToMatch.getExtractorPath() )
					.getInverseSidePath();
			if ( result.isPresent() ) {
				return result;
			}
		}
		return Optional.empty();
	}

	private Optional<PojoAssociationPath> findInverseSidePathFromInverseSide(
			PojoTypeModel<?> inverseSideTypeModel,
			PojoRawTypeModel<?> originalSideEntityType,
			List<PojoAssociationPath> associationPathsToMatch) {
		PojoAugmentedTypeModel augmentedInverseSideTypeModel =
				augmentedTypeModelProvider.get( inverseSideTypeModel.getRawType() );

		for ( Map.Entry<String, PojoAugmentedPropertyModel> propertyEntry :
				augmentedInverseSideTypeModel.getAugmentedProperties().entrySet() ) {
			String inverseSidePropertyName = propertyEntry.getKey();
			PojoPropertyModel<?> inverseSidePropertyModel = inverseSideTypeModel.getProperty( inverseSidePropertyName );
			PojoAugmentedPropertyModel augmentedInverseSidePropertyModel = propertyEntry.getValue();

			for ( Map.Entry<ContainerValueExtractorPath, PojoAugmentedValueModel> valueEntry :
					augmentedInverseSidePropertyModel.getAugmentedValues().entrySet() ) {
				ContainerValueExtractorPath inverseSideExtractorPath = valueEntry.getKey();
				PojoAugmentedValueModel augmentedInverseSideValueModel = valueEntry.getValue();

				Optional<PojoAssociationPath> candidatePathOptional =
						augmentedInverseSideValueModel.getInverseSidePath();

				if ( candidatePathOptional.isPresent()
						&& associationPathsToMatch.contains( candidatePathOptional.get() ) ) {
					/*
					 * In order to match, the inverse path, when applied to the inverse entity type,
					 * must also result in a supertype of the entity type holding the association to invert.
					 * This is to handle cases where an entity holds inverse associations of multiple associations
					 * from multiple different entities: in that case, the "original" associations may have
					 * the same name and extractors.
					 */
					BoundContainerValueExtractorPath<?, ?> resolvedExtractorPath =
							extractorBinder.bindPath(
									introspector, inverseSidePropertyModel.getTypeModel(),
									inverseSideExtractorPath
							);
					PojoRawTypeModel<?> rawExtractedTypeModel =
							resolvedExtractorPath.getExtractedType().getRawType();
					if ( originalSideEntityType.isSubTypeOf( rawExtractedTypeModel ) ) {
						PojoAssociationPath inverseAssociationPath = new PojoAssociationPath(
								inverseSidePropertyName, inverseSideExtractorPath
						);
						return Optional.of( inverseAssociationPath );
					}
				}
			}
		}

		return Optional.empty();
	}
}
