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
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverImpl;
import org.hibernate.search.mapper.pojo.automaticindexing.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorHolder;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilterFactory;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.impl.Closer;

public final class PojoImplicitReindexingResolverBuildingHelper {

	private final ContainerExtractorBinder extractorBinder;
	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;
	private final PojoAssociationPathInverter pathInverter;
	private final Set<PojoRawTypeModel<?>> entityTypes;
	private final ReindexOnUpdate defaultReindexOnUpdate;

	private final Map<PojoRawTypeModel<?>, Set<PojoRawTypeModel<?>>> concreteEntitySubTypesByEntitySuperType =
			new HashMap<>();
	private final Map<PojoRawTypeModel<?>, PojoImplicitReindexingResolverBuilder<?>> builderByType =
			new HashMap<>();

	public PojoImplicitReindexingResolverBuildingHelper(
			ContainerExtractorBinder extractorBinder,
			PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider,
			Set<PojoRawTypeModel<?>> entityTypes,
			ReindexOnUpdate defaultReindexOnUpdate) {
		this.extractorBinder = extractorBinder;
		this.typeAdditionalMetadataProvider = typeAdditionalMetadataProvider;
		this.pathInverter = new PojoAssociationPathInverter( typeAdditionalMetadataProvider, extractorBinder );
		this.entityTypes = entityTypes;
		this.defaultReindexOnUpdate = defaultReindexOnUpdate;

		for ( PojoRawTypeModel<?> entityType : entityTypes ) {
			if ( !entityType.isAbstract() ) {
				entityType.ascendingSuperTypes().forEach(
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

	public <T> PojoImplicitReindexingResolver<T> build(PojoRawTypeModel<T> typeModel,
			PojoPathFilterFactory pathFilterFactory) {
		return buildOptional( typeModel, pathFilterFactory )
				.orElseGet( () -> new PojoImplicitReindexingResolverImpl<>(
						PojoPathFilter.empty(), PojoImplicitReindexingResolverNode.noOp()
				) );
	}

	public <T> Optional<PojoImplicitReindexingResolver<T>> buildOptional(PojoRawTypeModel<T> typeModel,
			PojoPathFilterFactory pathFilterFactory) {
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

	PojoAssociationPathInverter pathInverter() {
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

	ContainerExtractorBinder extractorBinder() {
		return extractorBinder;
	}

	<V, T> ContainerExtractorHolder<T, V> createExtractors(
			BoundContainerExtractorPath<T, V> boundExtractorPath) {
		return extractorBinder.create( boundExtractorPath );
	}

	ReindexOnUpdate getDefaultReindexOnUpdate() {
		return defaultReindexOnUpdate;
	}

	ReindexOnUpdate getMetadataReindexOnUpdateOrNull(PojoTypeModel<?> typeModel,
			String propertyName, ContainerExtractorPath extractorPath) {
		PojoTypeAdditionalMetadata typeAdditionalMetadata =
				typeAdditionalMetadataProvider.get( typeModel.rawType() );
		Optional<ReindexOnUpdate> reindexOnUpdateOptional =
				typeAdditionalMetadata.getPropertyAdditionalMetadata( propertyName )
						.getValueAdditionalMetadata( extractorPath )
						.getReindexOnUpdate();
		if ( reindexOnUpdateOptional.isPresent() ) {
			return reindexOnUpdateOptional.get();
		}

		if ( extractorBinder.isDefaultExtractorPath( typeModel.property( propertyName ).typeModel(), extractorPath ) ) {
			reindexOnUpdateOptional = typeAdditionalMetadata.getPropertyAdditionalMetadata( propertyName )
					.getValueAdditionalMetadata( ContainerExtractorPath.defaultExtractors() )
					.getReindexOnUpdate();
		}

		return reindexOnUpdateOptional.orElse( null );
	}

	Set<PojoModelPathValueNode> getMetadataDerivedFrom(PojoTypeModel<?> typeModel, String propertyName,
			ContainerExtractorPath extractorPath) {
		PojoTypeAdditionalMetadata typeAdditionalMetadata =
				typeAdditionalMetadataProvider.get( typeModel.rawType() );
		Set<PojoModelPathValueNode> derivedFrom =
				typeAdditionalMetadata.getPropertyAdditionalMetadata( propertyName )
						.getValueAdditionalMetadata( extractorPath )
						.getDerivedFrom();
		if ( derivedFrom.isEmpty() ) {
			if ( extractorBinder.isDefaultExtractorPath(
					typeModel.property( propertyName ).typeModel(),
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
