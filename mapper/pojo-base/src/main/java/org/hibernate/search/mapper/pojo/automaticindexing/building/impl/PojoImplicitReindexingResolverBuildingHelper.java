/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorHolder;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.impl.Closer;

public final class PojoImplicitReindexingResolverBuildingHelper {

	private final ContainerExtractorBinder extractorBinder;
	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;
	private final PojoAssociationPathInverter pathInverter;
	private final Set<PojoRawTypeModel<?>> entityTypes;
	private final Map<PojoRawTypeModel<?>, Set<PojoRawTypeModel<?>>> concreteEntitySubTypesByEntitySuperType =
			new HashMap<>();
	private final Map<PojoRawTypeModel<?>, PojoImplicitReindexingResolverBuilder<?>> builderByType =
			new HashMap<>();

	public PojoImplicitReindexingResolverBuildingHelper(
			ContainerExtractorBinder extractorBinder,
			PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider,
			PojoAssociationPathInverter pathInverter,
			Set<PojoRawTypeModel<?>> entityTypes) {
		this.extractorBinder = extractorBinder;
		this.typeAdditionalMetadataProvider = typeAdditionalMetadataProvider;
		this.pathInverter = pathInverter;
		this.entityTypes = entityTypes;

		for ( PojoRawTypeModel<?> entityType : entityTypes ) {
			if ( !entityType.isAbstract() ) {
				entityType.getAscendingSuperTypes().forEach(
						superType ->
								concreteEntitySubTypesByEntitySuperType.computeIfAbsent(
										superType,
										// Use a LinkedHashSet for deterministic iteration
										ignored -> new LinkedHashSet<>()
								)
										.add( entityType )
				);
			}
		}
		// Make sure every Set is unmodifiable
		for ( Map.Entry<PojoRawTypeModel<?>, Set<PojoRawTypeModel<?>>> entry
				: concreteEntitySubTypesByEntitySuperType.entrySet() ) {
			entry.setValue( Collections.unmodifiableSet( entry.getValue() ) );
		}
	}

	public <T> PojoIndexingDependencyCollectorTypeNode<T> createDependencyCollector(PojoRawTypeModel<T> typeModel) {
		return new PojoIndexingDependencyCollectorTypeNode<>( typeModel, this );
	}

	public void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( PojoImplicitReindexingResolverBuilder::closeOnFailure, builderByType.values() );
		}
	}

	public <T, S> Optional<PojoImplicitReindexingResolver<T, S>> build(PojoRawTypeModel<T> typeModel,
			PojoPathFilterFactory<S> pathFilterFactory) {
		@SuppressWarnings("unchecked") // We know builders have this type, by construction
		PojoImplicitReindexingResolverBuilder<T> builder =
				(PojoImplicitReindexingResolverBuilder<T>) builderByType.get( typeModel );
		if ( builder == null ) {
			return Optional.empty();
		}
		else {
			return builder.build( pathFilterFactory );
		}
	}

	PojoAssociationPathInverter getPathInverter() {
		return pathInverter;
	}

	boolean isEntity(PojoRawTypeModel<?> typeModel) {
		return entityTypes.contains( typeModel );
	}

	/**
	 * @return The set of concrete entity types that extend the given type.
	 * This is useful when building resolvers: when a type is the target of an indexed-embedded association,
	 * we generally want to take this information into account for every concrete subtype of that type,
	 * because the association could target any of them at runtime.
	 */
	Set<? extends PojoRawTypeModel<?>> getConcreteEntitySubTypesForEntitySuperType(PojoRawTypeModel<?> superTypeModel) {
		return concreteEntitySubTypesByEntitySuperType.computeIfAbsent( superTypeModel, ignored -> Collections.emptySet() );
	}

	<T> PojoImplicitReindexingResolverBuilder<T> getOrCreateResolverBuilder(
			PojoRawTypeModel<T> rawTypeModel) {
		@SuppressWarnings("unchecked") // We know builders have this type, by construction
		PojoImplicitReindexingResolverBuilder<T> builder =
				(PojoImplicitReindexingResolverBuilder<T>) builderByType.get( rawTypeModel );
		if ( builder == null ) {
			builder = new PojoImplicitReindexingResolverBuilder<>(
					rawTypeModel, this
			);
			builderByType.put( rawTypeModel, builder );
		}
		return builder;
	}

	ContainerExtractorBinder getExtractorBinder() {
		return extractorBinder;
	}

	<T> BoundContainerExtractorPath<T, ?> bindExtractorPath(
			PojoGenericTypeModel<T> typeModel, ContainerExtractorPath extractorPath) {
		return extractorBinder.bindPath( typeModel, extractorPath );
	}

	<V, T> ContainerExtractorHolder<T, V> createExtractors(
			BoundContainerExtractorPath<T, V> boundExtractorPath) {
		return extractorBinder.create( boundExtractorPath );
	}

	ReindexOnUpdate getReindexOnUpdate(ReindexOnUpdate parentReindexOnUpdate,
			PojoTypeModel<?> typeModel, String propertyName, ContainerExtractorPath extractorPath) {
		if ( ReindexOnUpdate.NO.equals( parentReindexOnUpdate ) ) {
			return ReindexOnUpdate.NO;
		}
		else {
			PojoTypeAdditionalMetadata typeAdditionalMetadata =
					typeAdditionalMetadataProvider.get( typeModel.getRawType() );
			Optional<ReindexOnUpdate> reindexOnUpdateOptional =
					typeAdditionalMetadata.getPropertyAdditionalMetadata( propertyName )
							.getValueAdditionalMetadata( extractorPath )
							.getReindexOnUpdate();
			if ( !reindexOnUpdateOptional.isPresent() ) {
				if ( extractorBinder.isDefaultExtractorPath(
						typeModel.getProperty( propertyName ).getTypeModel(),
						extractorPath
				) ) {
					reindexOnUpdateOptional = typeAdditionalMetadata.getPropertyAdditionalMetadata( propertyName )
							.getValueAdditionalMetadata( ContainerExtractorPath.defaultExtractors() )
							.getReindexOnUpdate();
				}
			}

			return reindexOnUpdateOptional.orElse( ReindexOnUpdate.DEFAULT );
		}
	}

	Set<PojoModelPathValueNode> getDerivedFrom(PojoTypeModel<?> typeModel, String propertyName,
			ContainerExtractorPath extractorPath) {
		PojoTypeAdditionalMetadata typeAdditionalMetadata =
				typeAdditionalMetadataProvider.get( typeModel.getRawType() );
		Set<PojoModelPathValueNode> derivedFrom =
				typeAdditionalMetadata.getPropertyAdditionalMetadata( propertyName )
						.getValueAdditionalMetadata( extractorPath )
						.getDerivedFrom();
		if ( derivedFrom.isEmpty() ) {
			if ( extractorBinder.isDefaultExtractorPath(
					typeModel.getProperty( propertyName ).getTypeModel(),
					extractorPath
			) ) {
				derivedFrom = typeAdditionalMetadata.getPropertyAdditionalMetadata( propertyName )
						.getValueAdditionalMetadata( ContainerExtractorPath.defaultExtractors() )
						.getDerivedFrom();
			}
		}

		return derivedFrom;
	}
}
