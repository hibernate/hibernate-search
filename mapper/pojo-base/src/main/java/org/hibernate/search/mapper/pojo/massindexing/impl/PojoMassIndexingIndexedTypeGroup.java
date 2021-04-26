/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hibernate.search.mapper.pojo.loading.EntityIdentifierScroll;
import org.hibernate.search.mapper.pojo.loading.EntityLoader;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingEntityLoadingStrategy;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingThreadContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.loading.EntityLoadingTypeGroupingStrategy;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingEntityLoadingTypeGroup;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingMappingContext;
import org.hibernate.search.util.common.AssertionFailure;

public class PojoMassIndexingIndexedTypeGroup<E, O> {

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
	 * @param <O> The type of mass indexing options.
	 * @param indexingContext A conficuration contextural information.
	 * @param mappingContext A mappings contextural information.
	 * @param indexedTypeContexts A set of indexed types to group together.
	 * @return One or more type groups that are guaranteed to be disjoint.
	 */
	public static <O> List<PojoMassIndexingIndexedTypeGroup<?, O>> disjoint(PojoMassIndexingContext<O> indexingContext,
			PojoMassIndexingMappingContext mappingContext,
			PojoMassIndexingTypeContextProvider typeContextProvider,
			Set<? extends PojoMassIndexingIndexedTypeContext<?>> indexedTypeContexts) {
		List<PojoMassIndexingIndexedTypeGroup<?, O>> typeGroups = new ArrayList<>();
		for ( PojoMassIndexingIndexedTypeContext<?> typeContext : indexedTypeContexts ) {
			PojoMassIndexingIndexedTypeGroup<?, O> typeGroup = PojoMassIndexingIndexedTypeGroup.single( indexingContext,
					mappingContext, typeContextProvider, typeContext );
			// First try to merge this new type group with an existing one
			ListIterator<PojoMassIndexingIndexedTypeGroup<?, O>> iterator = typeGroups.listIterator();
			while ( iterator.hasNext() ) {
				PojoMassIndexingIndexedTypeGroup<?, O> mergeResult = iterator.next().mergeOrNull( typeGroup );
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

	private static <E, O> PojoMassIndexingIndexedTypeGroup<E, O> single(PojoMassIndexingContext<O> indexingContext,
			PojoMassIndexingMappingContext mappingContext,
			PojoMassIndexingTypeContextProvider typeContextProvider,
			PojoMassIndexingIndexedTypeContext<E> typeContext) {
		MassIndexingEntityLoadingStrategy<? super E, O> strategy =
				indexingContext.indexLoadingStrategy( typeContext.typeIdentifier() );
		return new PojoMassIndexingIndexedTypeGroup<>( typeContext, indexingContext, mappingContext, typeContextProvider,
				strategy, Collections.singleton( typeContext ) );
	}

	private final PojoMassIndexingIndexedTypeContext<E> commonSuperType;
	private final PojoMassIndexingContext<?> indexingContext;
	private final PojoMassIndexingMappingContext mappingContext;
	private final PojoMassIndexingTypeContextProvider typeContextProvider;
	private final MassIndexingEntityLoadingStrategy<? super E, O> loadingStrategy;
	private final Set<PojoMassIndexingIndexedTypeContext<? extends E>> includedTypes;

	private PojoMassIndexingIndexedTypeGroup(PojoMassIndexingIndexedTypeContext<E> commonSuperType,
			PojoMassIndexingContext<?> indexingContext,
			PojoMassIndexingMappingContext mappingContext,
			PojoMassIndexingTypeContextProvider typeContextProvider,
			MassIndexingEntityLoadingStrategy<? super E, O> loadingStrategy,
			Set<PojoMassIndexingIndexedTypeContext<? extends E>> includedTypes) {
		this.commonSuperType = commonSuperType;
		this.indexingContext = indexingContext;
		this.mappingContext = mappingContext;
		this.typeContextProvider = typeContextProvider;
		this.loadingStrategy = loadingStrategy;
		this.includedTypes = includedTypes;
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
		return String.join( ",", includedEntityMap().keySet() );
	}

	public Map<String, Class<? extends E>> includedEntityMap() {
		return includedTypes.stream()
				.collect( Collectors.toMap( PojoMassIndexingIndexedTypeContext::entityName, t -> t.typeIdentifier().javaClass(),
						(o1, o2) -> o1, LinkedHashMap::new ) );
	}

	public boolean includesInstance(PojoMassIndexingSessionContext sessionContext, Object entity) {
		PojoRawTypeIdentifier<?> targetType = sessionContext.runtimeIntrospector().detectEntityType( entity );
		Optional<? extends PojoMassIndexingIndexedTypeContext<?>> typeContextOptional =
				typeContextProvider.forExactType( targetType );
		if ( !typeContextOptional.isPresent() ) {
			return false;
		}
		PojoMassIndexingIndexedTypeContext<?> typeContext = typeContextOptional.get();
		return includedTypes.stream().anyMatch( Predicate.isEqual( typeContext ) );
	}

	public Object extractReference(PojoMassIndexingSessionContext sessionContext, Object entity) {
		PojoRawTypeIdentifier<?> targetType = sessionContext.runtimeIntrospector().detectEntityType( entity );
		PojoMassIndexingIndexedTypeContext<?> typeContext = typeContextProvider.forExactType( targetType )
				.orElseThrow( () -> new AssertionFailure(
						"Processing a non-indexed type in the MassIndexer: " + targetType ) );
		String entityName = typeContext.entityName();
		Object identifier = extractIdentifier( typeContext, sessionContext, entity );
		return mappingContext.entityReferenceFactory().createEntityReference( entityName, identifier );

	}

	public <E2> Object extractIdentifier(PojoMassIndexingIndexedTypeContext<E2> typeContext,
			PojoMassIndexingSessionContext sessionContext, Object entity) {
		return typeContext.identifierMapping().getIdentifier( null,
				typeContext.toEntitySupplier( sessionContext, entity ) );
	}

	EntityIdentifierScroll createIdentifierScroll(MassIndexingThreadContext<O> context, PojoMassIndexingSessionContext sessionContext) throws InterruptedException {
		return loadingStrategy.createIdentifierScroll( context, createLoadingTypeGroup( sessionContext ) );
	}

	EntityLoader<? super E> createLoader(MassIndexingThreadContext<O> context, PojoMassIndexingSessionContext sessionContext) throws InterruptedException {
		return loadingStrategy.createLoader( context, createLoadingTypeGroup( sessionContext ) );
	}

	private MassIndexingEntityLoadingTypeGroup<E> createLoadingTypeGroup(PojoMassIndexingSessionContext sessionContext) {
		return new MassIndexingEntityLoadingTypeGroup<E>() {

			@Override
			public Map<String, Class<? extends E>> includedEntityMap() {
				return PojoMassIndexingIndexedTypeGroup.this.includedEntityMap();
			}

			@Override
			public boolean includesInstance(Object entity) {
				return PojoMassIndexingIndexedTypeGroup.this.includesInstance( sessionContext, entity );
			}

			@Override
			public String toString() {
				return PojoMassIndexingIndexedTypeGroup.this.notifiedGroupName();
			}
		};
	}

	/**
	 * Merge this group with the other group if
	 * the other group uses the same loading strategy and
	 * the other group's {@code commonSuperType} represents a supertype or subtype
	 * of this group's {@code commonSuperType}.
	 * @param other The other group to merge with (if possible).
	 * @return The merged group, or {@code null} if
	 * the other group uses a different loading strategy or
	 * the other group's {@code commonSuperType} does <strong>not</strong> represent
	 * a supertype or subtype of this group's {@code commonSuperType}.
	 */
	@SuppressWarnings("unchecked") // The casts are guarded by reflection checks
	private PojoMassIndexingIndexedTypeGroup<?, O> mergeOrNull(PojoMassIndexingIndexedTypeGroup<?, O> other) {
		if ( !loadingStrategy.equals( other.loadingStrategy ) ) {
			return null;
		}
		EntityLoadingTypeGroupingStrategy.GroupingType groupingType = loadingStrategy.groupingStrategy().get(
				commonSuperType.entityName(), commonSuperType.typeIdentifier().javaClass(),
				other.commonSuperType.entityName(), other.commonSuperType.typeIdentifier().javaClass() );

		if ( groupingType == EntityLoadingTypeGroupingStrategy.GroupingType.SUPER ) {
			return withAdditionalTypes( ((PojoMassIndexingIndexedTypeGroup<? extends E, O>) other).includedTypes );
		}
		if ( groupingType == EntityLoadingTypeGroupingStrategy.GroupingType.INCLUDED ) {
			return ((PojoMassIndexingIndexedTypeGroup<? super E, O>) other).withAdditionalTypes( includedTypes );
		}
		return null;
	}

	private PojoMassIndexingIndexedTypeGroup<E, O> withAdditionalTypes(
			Set<? extends PojoMassIndexingIndexedTypeContext<? extends E>> otherIncludedSubTypes) {
		Set<PojoMassIndexingIndexedTypeContext<? extends E>> mergedIncludedSubTypes
				= new LinkedHashSet<>( includedTypes );
		mergedIncludedSubTypes.addAll( otherIncludedSubTypes );
		return new PojoMassIndexingIndexedTypeGroup<>( commonSuperType, indexingContext, mappingContext,
				typeContextProvider, loadingStrategy, mergedIncludedSubTypes );
	}
}
