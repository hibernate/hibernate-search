/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.loading.impl.EntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmQueryLoader;

class MassIndexingIndexedTypeGroup<E, I> {

	/**
	 * Group indexed types by their closest common supertype,
	 * ensuring returned groups are disjoint
	 * (i.e. no two groups have any common indexed subtype among those provided).
	 * <p>
	 * This is necessary to avoid duplicate indexing.
	 * <p>
	 * For example, without this, we could end up reindexing type B in one thread,
	 * and its superclass A (which will include all instances of B) in another.
	 *
	 * @param indexedTypeContexts A set of indexed types to group together.
	 * @return One or more type groups that are guaranteed to be disjoint.
	 */
	public static List<MassIndexingIndexedTypeGroup<?, ?>> disjoint(
			Set<? extends HibernateOrmMassIndexingIndexedTypeContext<?>> indexedTypeContexts) {
		List<MassIndexingIndexedTypeGroup<?, ?>> typeGroups = new ArrayList<>();
		for ( HibernateOrmMassIndexingIndexedTypeContext<?> typeContext : indexedTypeContexts ) {
			MassIndexingIndexedTypeGroup<?, ?> typeGroup = MassIndexingIndexedTypeGroup.single( typeContext );
			// First try to merge this new type group with an existing one
			ListIterator<MassIndexingIndexedTypeGroup<?, ?>> iterator = typeGroups.listIterator();
			while ( iterator.hasNext() ) {
				MassIndexingIndexedTypeGroup<?, ?> mergeResult = iterator.next().mergeOrNull( typeGroup );
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

	private static <E> MassIndexingIndexedTypeGroup<E, ?> single(HibernateOrmMassIndexingIndexedTypeContext<E> typeContext) {
		return new MassIndexingIndexedTypeGroup<>( typeContext, typeContext.loadingStrategy(),
				Collections.singleton( typeContext ) );
	}

	private final HibernateOrmMassIndexingIndexedTypeContext<E> commonSuperType;
	private final EntityLoadingStrategy<? super E, I> loadingStrategy;
	private final Set<HibernateOrmMassIndexingIndexedTypeContext<? extends E>> includedTypes;

	private MassIndexingIndexedTypeGroup(HibernateOrmMassIndexingIndexedTypeContext<E> commonSuperType,
			EntityLoadingStrategy<? super E, I> loadingStrategy,
			Set<HibernateOrmMassIndexingIndexedTypeContext<? extends E>> includedTypes) {
		this.commonSuperType = commonSuperType;
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

	public HibernateOrmMassIndexingIndexedTypeContext<E> commonSuperType() {
		return commonSuperType;
	}

	public String includedEntityNames() {
		return includedTypes.stream().map( HibernateOrmMassIndexingIndexedTypeContext::jpaEntityName )
				.collect( Collectors.joining( "," ) );
	}

	public HibernateOrmQueryLoader<? super E, I> createLoader() {
		return loadingStrategy.createLoader( includedTypes );
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
	private MassIndexingIndexedTypeGroup<?, ?> mergeOrNull(MassIndexingIndexedTypeGroup<?, ?> other) {
		if ( !loadingStrategy.equals( other.loadingStrategy ) ) {
			return null;
		}
		EntityPersister entityPersister = commonSuperType.entityPersister();
		EntityPersister otherEntityPersister = other.commonSuperType.entityPersister();
		if ( HibernateOrmUtils.isSuperTypeOf( entityPersister, otherEntityPersister ) ) {
			return withAdditionalTypes( ( (MassIndexingIndexedTypeGroup<? extends E, I>) other ).includedTypes );
		}
		if ( HibernateOrmUtils.isSuperTypeOf( otherEntityPersister, entityPersister ) ) {
			return ( (MassIndexingIndexedTypeGroup<? super E, I>) other ).withAdditionalTypes( includedTypes );
		}
		return null;
	}

	private MassIndexingIndexedTypeGroup<E, I> withAdditionalTypes(
			Set<? extends HibernateOrmMassIndexingIndexedTypeContext<? extends E>> otherIncludedSubTypes) {
		Set<HibernateOrmMassIndexingIndexedTypeContext<? extends E>> mergedIncludedSubTypes =
				new HashSet<>( includedTypes );
		mergedIncludedSubTypes.addAll( otherIncludedSubTypes );
		return new MassIndexingIndexedTypeGroup<>( commonSuperType, loadingStrategy, mergedIncludedSubTypes );
	}
}
