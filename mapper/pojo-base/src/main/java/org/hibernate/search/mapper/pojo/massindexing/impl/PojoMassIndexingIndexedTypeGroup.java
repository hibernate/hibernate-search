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
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingContext;
import org.hibernate.search.mapper.pojo.loading.EntityLoadingTypeGroupStrategy;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingEntityLoadingTypeGroup;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingMappingContext;
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
	public static <O> List<PojoMassIndexingIndexedTypeGroup<?, O>> disjoint(MassIndexingContext<O> indexingContext,
			MassIndexingMappingContext mappingContext,
			MassIndexingTypeContextProvider typeContextProvider,
			Set<? extends MassIndexingIndexedTypeContext<?>> indexedTypeContexts) {
		List<PojoMassIndexingIndexedTypeGroup<?, O>> typeGroups = new ArrayList<>();
		for ( MassIndexingIndexedTypeContext<?> typeContext : indexedTypeContexts ) {
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

	private static <E, O> PojoMassIndexingIndexedTypeGroup<E, O> single(MassIndexingContext<O> indexingContext,
			MassIndexingMappingContext mappingContext,
			MassIndexingTypeContextProvider typeContextProvider,
			MassIndexingIndexedTypeContext<E> typeContext) {
		MassIndexingEntityLoadingStrategy<E, O> strategy =
				indexingContext.createIndexLoadingStrategy( typeContext.typeIdentifier() );
		return new PojoMassIndexingIndexedTypeGroup<>( typeContext, indexingContext, mappingContext, typeContextProvider,
				strategy, Collections.singleton( typeContext ) );
	}

	private final MassIndexingIndexedTypeContext<E> commonSuperType;
	private final MassIndexingContext<?> indexingContext;
	private final MassIndexingMappingContext mappingContext;
	private final MassIndexingTypeContextProvider typeContextProvider;
	private final MassIndexingEntityLoadingStrategy<E, O> loadingStrategy;
	private final Set<MassIndexingIndexedTypeContext<? extends E>> includedTypes;

	private PojoMassIndexingIndexedTypeGroup(MassIndexingIndexedTypeContext<E> commonSuperType,
			MassIndexingContext<?> indexingContext,
			MassIndexingMappingContext mappingContext,
			MassIndexingTypeContextProvider typeContextProvider,
			MassIndexingEntityLoadingStrategy<E, O> loadingStrategy,
			Set<MassIndexingIndexedTypeContext<? extends E>> includedTypes) {
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
				.collect( Collectors.toMap( MassIndexingIndexedTypeContext::entityName, t -> t.typeIdentifier().javaClass(),
						(o1, o2) -> o1, LinkedHashMap::new ) );
	}

	public boolean includesInstance(MassIndexingSessionContext sessionContext, Object entity) {
		PojoRawTypeIdentifier<?> targetType = sessionContext.runtimeIntrospector().detectEntityType( entity );
		Optional<? extends MassIndexingIndexedTypeContext<?>> typeContextOptional =
				typeContextProvider.forExactType( targetType );
		if ( !typeContextOptional.isPresent() ) {
			return false;
		}
		MassIndexingIndexedTypeContext<?> typeContext = typeContextOptional.get();
		return includedTypes.stream().anyMatch( Predicate.isEqual( typeContext ) );
	}

	public Object extractReference(MassIndexingSessionContext sessionContext, Object entity) {
		PojoRawTypeIdentifier<?> targetType = sessionContext.runtimeIntrospector().detectEntityType( entity );
		MassIndexingIndexedTypeContext<?> typeContext = typeContextProvider.forExactType( targetType )
				.orElseThrow( () -> new AssertionFailure(
						"Processing a non-indexed type in the MassIndexer: " + targetType ) );
		String entityName = typeContext.entityName();
		Object identifier = extractIdentifier( typeContext, sessionContext, entity );
		return mappingContext.entityReferenceFactory().createEntityReference( entityName, identifier );

	}

	public <E2> Object extractIdentifier(MassIndexingIndexedTypeContext<E2> typeContext,
			MassIndexingSessionContext sessionContext, Object entity) {
		return typeContext.identifierMapping().getIdentifier( null,
				typeContext.toEntitySupplier( sessionContext, entity ) );
	}

	EntityIdentifierScroll createIdentifierScroll(MassIndexingThreadContext<O> context, MassIndexingSessionContext sessionContext) throws InterruptedException {
		return loadingStrategy.createIdentifierScroll( context, createLoadingTypeGroup( sessionContext ) );
	}

	EntityLoader<E> createLoader(MassIndexingThreadContext<O> context, MassIndexingSessionContext sessionContext) throws InterruptedException {
		return loadingStrategy.createLoader( context, createLoadingTypeGroup( sessionContext ) );
	}

	private MassIndexingEntityLoadingTypeGroup<E> createLoadingTypeGroup(MassIndexingSessionContext sessionContext) {
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
		EntityLoadingTypeGroupStrategy.GroupingType groupingType = loadingStrategy.groupStrategy().get(
				commonSuperType.entityName(), commonSuperType.typeIdentifier().javaClass(),
				other.commonSuperType.entityName(), other.commonSuperType.typeIdentifier().javaClass() );

		if ( groupingType == EntityLoadingTypeGroupStrategy.GroupingType.SUPER ) {
			return withAdditionalTypes( ((PojoMassIndexingIndexedTypeGroup<? extends E, O>) other).includedTypes );
		}
		if ( groupingType == EntityLoadingTypeGroupStrategy.GroupingType.INCLUDED ) {
			return ((PojoMassIndexingIndexedTypeGroup<? super E, O>) other).withAdditionalTypes( includedTypes );
		}
		return null;
	}

	private PojoMassIndexingIndexedTypeGroup<E, O> withAdditionalTypes(
			Set<? extends MassIndexingIndexedTypeContext<? extends E>> otherIncludedSubTypes) {
		Set<MassIndexingIndexedTypeContext<? extends E>> mergedIncludedSubTypes
				= new LinkedHashSet<>( includedTypes );
		mergedIncludedSubTypes.addAll( otherIncludedSubTypes );
		return new PojoMassIndexingIndexedTypeGroup<>( commonSuperType, indexingContext, mappingContext,
				typeContextProvider, loadingStrategy, mergedIncludedSubTypes );
	}
}
