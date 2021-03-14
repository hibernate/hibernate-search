/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.search.mapper.pojo.loading.EntityIdentifierScroll;
import org.hibernate.search.mapper.pojo.loading.EntityLoader;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingEntityLoadingStrategy;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingThreadContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingContext;
import org.hibernate.search.mapper.pojo.loading.EntityLoadingTypeGroup;

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
	 * @param indexingContext A conficuration contextural information.
	 * @param indexedTypeContexts A set of indexed types to group together.
	 * @return One or more type groups that are guaranteed to be disjoint.
	 */
	public static List<PojoMassIndexingIndexedTypeGroup<?>> disjoint(MassIndexingContext indexingContext,
			Set<? extends PojoRawTypeIdentifier<?>> indexedTypeContexts) {
		List<PojoMassIndexingIndexedTypeGroup<?>> typeGroups = new ArrayList<>();
		for ( PojoRawTypeIdentifier<?> typeContext : indexedTypeContexts ) {
			PojoMassIndexingIndexedTypeGroup<?> typeGroup = PojoMassIndexingIndexedTypeGroup.single( indexingContext, typeContext );
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

	private static <E> PojoMassIndexingIndexedTypeGroup<E> single(MassIndexingContext indexingContext, PojoRawTypeIdentifier<E> typeContext) {
		MassIndexingEntityLoadingStrategy strategy = indexingContext.createIndexLoadingStrategy(
				(PojoRawTypeIdentifier) indexingContext.indexingKey( typeContext ) );
		return new PojoMassIndexingIndexedTypeGroup<>( typeContext, indexingContext, strategy,
				Collections.singleton( typeContext ) );
	}

	private final PojoRawTypeIdentifier<E> commonSuperType;
	private final String commonSuperName;
	private final MassIndexingContext indexingContext;
	private final MassIndexingEntityLoadingStrategy loadingStrategy;
	private final Set<PojoRawTypeIdentifier<? extends E>> includedTypes;
	private String includedEntityNames;

	private PojoMassIndexingIndexedTypeGroup(PojoRawTypeIdentifier<E> commonSuperType,
			MassIndexingContext indexingContext,
			MassIndexingEntityLoadingStrategy loadingStrategy,
			Set<PojoRawTypeIdentifier<? extends E>> includedTypes) {
		this.commonSuperType = commonSuperType;
		this.indexingContext = indexingContext;
		this.loadingStrategy = loadingStrategy;
		this.includedTypes = includedTypes;
		this.commonSuperName = indexingContext.entityName( commonSuperType );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "commonSuperType=" + commonSuperType
				+ ", loadingStrategy=" + includedTypes
				+ ", includedSubTypes=" + includedTypes
				+ "]";
	}

	public PojoRawTypeIdentifier<E> commonSuperType() {
		return commonSuperType;
	}

	public String commonSuperTypeEntityName() {
		return indexingContext.entityName( commonSuperType );
	}

	public Object extractReferenceOrSuppress(MassIndexingSessionContext sessionContext, Object entity, Throwable throwable) {
		return indexingContext.extractReferenceOrSuppress( sessionContext, commonSuperType, entity, throwable );
	}

	public String includedEntityNames() {
		if ( includedEntityNames == null ) {
			includedEntityNames = includedTypes.stream()
					.map( indexingContext::entityName )
					.collect( Collectors.joining( "," ) );

		}
		return includedEntityNames;
	}

	public MassIndexingEntityLoadingStrategy loadingStrategy() {
		return loadingStrategy;
	}

	EntityIdentifierScroll createIdentifierScroll(MassIndexingThreadContext context) throws InterruptedException {
		return loadingStrategy.createIdentifierScroll( context, includedTypes.stream()
				.map( PojoRawTypeIdentifier::javaClass ).collect( Collectors.toSet() ) );
	}

	EntityLoader createLoader(MassIndexingThreadContext context) throws InterruptedException {
		return loadingStrategy.createLoader( context, includedTypes.stream()
				.map( PojoRawTypeIdentifier::javaClass ).collect( Collectors.toSet() ) );
	}

	Object entityIdentifier(MassIndexingSessionContext sessionContext, Object entity) {
		return indexingContext.entityIdentifier( sessionContext, commonSuperType, entity );
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
	private PojoMassIndexingIndexedTypeGroup<?> mergeOrNull(PojoMassIndexingIndexedTypeGroup<?> other) {
		if ( !loadingStrategy.equals( other.loadingStrategy ) ) {
			return null;
		}
		EntityLoadingTypeGroup.GroupingType groupingType = loadingStrategy.assignGroup().copare( commonSuperName, commonSuperType.javaClass(),
				other.commonSuperName, other.commonSuperType.javaClass() );

		if ( groupingType == EntityLoadingTypeGroup.GroupingType.SUPER ) {
			return withAdditionalTypes( ((PojoMassIndexingIndexedTypeGroup<? extends E>) other).includedTypes );
		}
		if ( groupingType == EntityLoadingTypeGroup.GroupingType.INCLUDED ) {
			return ((PojoMassIndexingIndexedTypeGroup<? super E>) other).withAdditionalTypes( includedTypes );
		}
		return null;
	}

	private PojoMassIndexingIndexedTypeGroup<E> withAdditionalTypes(
			Set<? extends PojoRawTypeIdentifier<? extends E>> otherIncludedSubTypes) {
		Set<PojoRawTypeIdentifier<? extends E>> mergedIncludedSubTypes
				= new HashSet<>( includedTypes );
		mergedIncludedSubTypes.addAll( otherIncludedSubTypes );
		return new PojoMassIndexingIndexedTypeGroup<>( commonSuperType, indexingContext, loadingStrategy, mergedIncludedSubTypes );
	}
}
