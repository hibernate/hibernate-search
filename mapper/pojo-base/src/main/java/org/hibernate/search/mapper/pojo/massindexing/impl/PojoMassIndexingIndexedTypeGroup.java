/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingStrategy;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingMappingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public class PojoMassIndexingIndexedTypeGroup<E> {

	/**
	 * Group indexed types by their closest common supertype,
	 * ensuring returned groups are disjoint
	 * (i.e.no two groups have any common indexed subtype among those provided)
	 * .<p>
	 * This is necessary to avoid duplicate indexing.
	 * <p>
	 * For example, without this, we could end up reindexing type B in one thread,
	 * and its superclass A (which will include all instances of B) in another.
	 *
	 * @param mappingContext The mapping context.
	 * @param typeContextProvider A provider of type contexts, used for some reflection operations.
	 * @param indexedTypeContexts A set of indexed types to group together.
	 * @param loadingContext Mapper-specific loading context.
	 *
	 * @return One or more type groups that are guaranteed to be disjoint.
	 */
	public static List<PojoMassIndexingIndexedTypeGroup<?>> disjoint(PojoMassIndexingMappingContext mappingContext,
			PojoMassIndexingTypeContextProvider typeContextProvider,
			Set<? extends PojoMassIndexingIndexedTypeContext<?>> indexedTypeContexts,
			PojoMassLoadingContext loadingContext) {
		List<PojoMassIndexingIndexedTypeGroup<?>> typeGroups = new ArrayList<>();
		for ( PojoMassIndexingIndexedTypeContext<?> typeContext : indexedTypeContexts ) {
			PojoMassIndexingIndexedTypeGroup<?> typeGroup = PojoMassIndexingIndexedTypeGroup.single(
					mappingContext, typeContextProvider, typeContext, loadingContext );
			// First try to merge this new type group with an existing one
			ListIterator<PojoMassIndexingIndexedTypeGroup<?>> iterator = typeGroups.listIterator();
			while ( iterator.hasNext() ) {
				PojoMassIndexingIndexedTypeGroup<?> mergeResult = iterator.next().mergeOrNull( typeGroup );
				if ( mergeResult != null ) {
					// We found an existing group that can be merged with this one.
					// Remove that group, we'll add the merge result to the list later.
					typeGroup = mergeResult;
					iterator.remove();
					// Continue iterating through existing groups, as we may be able to merge with multiple groups.
				}
			}
			typeGroups.add( typeGroup );
		}
		return typeGroups;
	}

	private static <E> PojoMassIndexingIndexedTypeGroup<? super E> single(PojoMassIndexingMappingContext mappingContext,
			PojoMassIndexingTypeContextProvider typeContextProvider, PojoMassIndexingIndexedTypeContext<E> typeContext,
			PojoMassLoadingContext loadingContext) {
		PojoMassLoadingStrategy<? super E, ?> strategy = typeContext.massLoadingStrategy();
		return new PojoMassIndexingIndexedTypeGroup<>( strategy, typeContext, mappingContext, typeContextProvider,
				Collections.singleton( typeContext ), loadingContext );
	}

	private final PojoMassLoadingStrategy<E, ?> loadingStrategy;
	private final PojoMassIndexingIndexedTypeContext<? extends E> commonSuperType;
	private final PojoMassIndexingMappingContext mappingContext;
	private final PojoMassIndexingTypeContextProvider typeContextProvider;
	private final Set<PojoMassIndexingIndexedTypeContext<? extends E>> includedTypes;
	private final PojoMassLoadingContext loadingContext;
	private Boolean groupingAllowed;

	private PojoMassIndexingIndexedTypeGroup(PojoMassLoadingStrategy<E, ?> loadingStrategy,
			PojoMassIndexingIndexedTypeContext<? extends E> commonSuperType,
			PojoMassIndexingMappingContext mappingContext,
			PojoMassIndexingTypeContextProvider typeContextProvider,
			Set<PojoMassIndexingIndexedTypeContext<? extends E>> includedTypes,
			PojoMassLoadingContext loadingContext) {
		this.commonSuperType = commonSuperType;
		this.mappingContext = mappingContext;
		this.typeContextProvider = typeContextProvider;
		this.loadingStrategy = loadingStrategy;
		this.includedTypes = includedTypes;
		this.loadingContext = loadingContext;
		if ( includedTypes.size() > 1 ) {
			// We're already grouping, so obviously it's allowed.
			groupingAllowed = true;
		}
		// else: evaluate groupingAllowed lazily (see getter)
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "commonSuperType=" + commonSuperType
				+ ", loadingStrategy=" + includedTypes
				+ ", includedSubTypes=" + includedTypes
				+ "]";
	}

	public String notifiedGroupName() {
		return includedTypes.stream().map( PojoMassIndexingIndexedTypeContext::entityName )
				.collect( Collectors.joining( "," ) );
	}

	public Set<PojoMassIndexingIndexedTypeContext<? extends E>> includedTypes() {
		return includedTypes;
	}

	private boolean groupingAllowed() {
		if ( groupingAllowed == null ) {
			groupingAllowed = loadingStrategy.groupingAllowed( includedTypes.iterator().next(), loadingContext );
		}
		return groupingAllowed;
	}

	public EntityReference extractReference(PojoMassIndexingSessionContext sessionContext, Object entity) {
		PojoRawTypeIdentifier<?> targetType = sessionContext.runtimeIntrospector().detectEntityType( entity );
		PojoMassIndexingIndexedTypeContext<?> typeContext = typeContextProvider.indexedForExactType( targetType );
		String entityName = typeContext.entityName();
		Object identifier = extractIdentifier( typeContext, sessionContext, entity );
		return mappingContext.entityReferenceFactoryDelegate().create( targetType, entityName, identifier );
	}

	public EntityReference makeSuperTypeReference(Object identifier) {
		return mappingContext.entityReferenceFactoryDelegate().create( commonSuperType.typeIdentifier(),
				commonSuperType.entityName(), identifier );
	}

	public <E2> Object extractIdentifier(PojoMassIndexingIndexedTypeContext<E2> typeContext,
			PojoMassIndexingSessionContext sessionContext, Object entity) {
		return typeContext.identifierMapping().getIdentifier( null,
				typeContext.toEntitySupplier( sessionContext, entity ) );
	}

	public PojoMassLoadingStrategy<E, ?> loadingStrategy() {
		return loadingStrategy;
	}

	/**
	 * Merge this group with the other group if
	 * the other group uses the same loading strategy,
	 * and the loading strategy {@link PojoMassLoadingStrategy#groupingAllowed allows grouping for these types},
	 * and the other group's {@code commonSuperType} represents a supertype or subtype
	 * of this group's {@code commonSuperType}.
	 * @param other The other group to merge with (if possible).
	 * @return The merged group, or {@code null} if
	 * the other group uses a different loading strategy or
	 * the other group's {@code commonSuperType} does <strong>not</strong> represent
	 * a supertype or subtype of this group's {@code commonSuperType}.
	 */
	@SuppressWarnings("unchecked") // The casts are guarded by reflection checks
	private PojoMassIndexingIndexedTypeGroup<?> mergeOrNull(PojoMassIndexingIndexedTypeGroup<?> other) {
		if ( !loadingStrategy.equals( other.loadingStrategy ) ) {
			return null;
		}
		if ( !groupingAllowed() || !other.groupingAllowed() ) {
			return null;
		}
		// We know both types are indexed types, which means both types are entity types,
		// so if they share the same loading strategy, we can assume they also share the same identifier space.
		// If one is the supertype of the other, make sure to load them in the same group:
		// if all subtypes are included in a group, it should perform better.
		if ( isFirstSuperTypeOfSecond( commonSuperType, other.commonSuperType ) ) {
			return withAdditionalTypes( ( (PojoMassIndexingIndexedTypeGroup<? extends E>) other ).includedTypes );
		}
		else if ( isFirstSuperTypeOfSecond( other.commonSuperType, commonSuperType ) ) {
			return ( (PojoMassIndexingIndexedTypeGroup<? super E>) other ).withAdditionalTypes( includedTypes );
		}
		else {
			return null;
		}
	}

	private boolean isFirstSuperTypeOfSecond(PojoMassIndexingIndexedTypeContext<?> first,
			PojoMassIndexingIndexedTypeContext<?> second) {
		return typeContextProvider.indexedForSuperType( first.typeIdentifier() )
				.map( s -> s.contains( second ) )
				.orElse( false );
	}

	private PojoMassIndexingIndexedTypeGroup<E> withAdditionalTypes(
			Set<? extends PojoMassIndexingIndexedTypeContext<? extends E>> otherIncludedSubTypes) {
		Set<PojoMassIndexingIndexedTypeContext<? extends E>> mergedIncludedSubTypes = new LinkedHashSet<>( includedTypes );
		mergedIncludedSubTypes.addAll( otherIncludedSubTypes );
		return new PojoMassIndexingIndexedTypeGroup<>( loadingStrategy, commonSuperType, mappingContext,
				typeContextProvider, mergedIncludedSubTypes, loadingContext
		);
	}
}
