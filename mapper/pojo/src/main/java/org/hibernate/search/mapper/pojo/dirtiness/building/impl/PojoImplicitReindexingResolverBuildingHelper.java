/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolver;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerValueExtractorBinder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public final class PojoImplicitReindexingResolverBuildingHelper {

	private final PojoBootstrapIntrospector introspector;
	private final ContainerValueExtractorBinder extractorBinder;
	private final PojoAssociationPathInverter pathInverter;
	private final Set<PojoRawTypeModel<?>> entityTypes;
	private final Map<PojoRawTypeModel<?>, Set<PojoRawTypeModel<?>>> concreteEntitySubTypesByEntitySuperType =
			new HashMap<>();
	private final Map<PojoRawTypeModel<?>, PojoImplicitReindexingResolverOriginalTypeNodeBuilder<?>> builderByType =
			new HashMap<>();

	public PojoImplicitReindexingResolverBuildingHelper(
			PojoAssociationPathInverter pathInverter,
			PojoBootstrapIntrospector introspector,
			ContainerValueExtractorBinder extractorBinder,
			Set<PojoRawTypeModel<?>> entityTypes) {
		this.introspector = introspector;
		this.extractorBinder = extractorBinder;
		this.pathInverter = pathInverter;
		this.entityTypes = entityTypes;

		for ( PojoRawTypeModel<?> entityType : entityTypes ) {
			if ( !entityType.isAbstract() ) {
				entityType.getAscendingSuperTypes().forEach(
						superType ->
								concreteEntitySubTypesByEntitySuperType.computeIfAbsent( superType, ignored -> new HashSet<>() )
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

	public <T> Optional<PojoImplicitReindexingResolver<T>> build(PojoRawTypeModel<T> typeModel) {
		@SuppressWarnings("unchecked") // We know builders have this type, by construction
		PojoImplicitReindexingResolverOriginalTypeNodeBuilder<T> builder =
				(PojoImplicitReindexingResolverOriginalTypeNodeBuilder<T>) builderByType.get( typeModel );
		if ( builder == null ) {
			return Optional.empty();
		}
		else {
			return builder.build();
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

	<T> PojoImplicitReindexingResolverOriginalTypeNodeBuilder<T> getOrCreateResolverBuilder(
			PojoRawTypeModel<T> rawTypeModel) {
		@SuppressWarnings("unchecked") // We know builders have this type, by construction
		PojoImplicitReindexingResolverOriginalTypeNodeBuilder<T> builder =
				(PojoImplicitReindexingResolverOriginalTypeNodeBuilder<T>) builderByType.get( rawTypeModel );
		if ( builder == null ) {
			builder = new PojoImplicitReindexingResolverOriginalTypeNodeBuilder<>(
					BoundPojoModelPath.root( rawTypeModel ), this
			);
			builderByType.put( rawTypeModel, builder );
		}
		return builder;
	}

	<T> BoundContainerValueExtractorPath<T,?> bindExtractorPath(
			PojoGenericTypeModel<T> typeModel, ContainerValueExtractorPath extractorPath) {
		return extractorBinder.bindPath( introspector, typeModel, extractorPath );
	}

	<V, T> ContainerValueExtractor<? super T,V> createExtractors(
			BoundContainerValueExtractorPath<T, V> boundExtractorPath) {
		return extractorBinder.create( boundExtractorPath );
	}
}
