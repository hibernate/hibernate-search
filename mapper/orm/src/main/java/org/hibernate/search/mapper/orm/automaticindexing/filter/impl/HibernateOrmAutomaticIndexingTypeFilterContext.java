/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.filter.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContextProvider;
import org.hibernate.search.mapper.pojo.automaticindexing.filter.PojoAutomaticIndexingTypeFilterContext;
import org.hibernate.search.mapper.pojo.automaticindexing.filter.spi.PojoAutomaticIndexingTypeFilterHolder;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class HibernateOrmAutomaticIndexingTypeFilterContext implements PojoAutomaticIndexingTypeFilterContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmSessionTypeContextProvider contextProvider;

	private final Set<PojoRawTypeIdentifier<?>> includes = new HashSet<>();
	private final Set<PojoRawTypeIdentifier<?>> excludes = new HashSet<>();

	public HibernateOrmAutomaticIndexingTypeFilterContext(HibernateOrmSessionTypeContextProvider typeManager) {
		this.contextProvider = typeManager;
	}

	@Override
	public PojoAutomaticIndexingTypeFilterContext include(String name) {
		addIfNotPresentInOther(
				contextProvider.byEntityName().getOrFail( name ).typeIdentifier(),
				includes,
				excludes
		);
		return this;
	}

	@Override
	public PojoAutomaticIndexingTypeFilterContext include(Class<?> clazz) {
		addIfNotPresentInOther(
				contextProvider.typeIdentifierResolver().resolveByJavaClass( clazz ),
				includes,
				excludes
		);
		return this;
	}

	@Override
	public PojoAutomaticIndexingTypeFilterContext exclude(String name) {
		addIfNotPresentInOther(
				contextProvider.byEntityName().getOrFail( name ).typeIdentifier(),
				excludes,
				includes
		);
		return this;
	}

	@Override
	public PojoAutomaticIndexingTypeFilterContext exclude(Class<?> clazz) {
		addIfNotPresentInOther(
				contextProvider.typeIdentifierResolver().resolveByJavaClass( clazz ),
				excludes,
				includes
		);
		return this;
	}

	public HibernateOrmAutomaticIndexingTypeFilter createFilter() {
		return createFilter( null );
	}

	public HibernateOrmAutomaticIndexingTypeFilter createFilter(PojoAutomaticIndexingTypeFilterHolder fallback) {
		Set<PojoRawTypeIdentifier<?>> allIncludes = new HashSet<>();
		Set<PojoRawTypeIdentifier<?>> allExcludes = new HashSet<>();
		boolean allTypesProcessed = true;

		for ( PojoTypeContext<?> typeContext : contextProvider.byEntityName().values() ) {
			PojoRawTypeIdentifier<?> typedIdentifier = typeContext.typeIdentifier();

			//.include(...) => included no matter what; subclasses included unless excluded explicitly in the same filter
			//.exclude(...) => excluded no matter what; subclasses excluded unless included explicitly in the same filter
			//.include(...) + .exclude(...) for the same entity in the same filter => exception
			// neither .include(...) nor .exclude(...) => defer to the superclass inclusions/exclusions,
			// then to the application filter, with the same behavior, and by default include
			if ( excludes.contains( typedIdentifier ) ) {
				allExcludes.add( typedIdentifier );
			}
			else if ( includes.contains( typedIdentifier ) ) {
				allIncludes.add( typedIdentifier );
			}
			else {
				Optional<Class<?>> closestInclude = findClosestClass( typedIdentifier.javaClass(), includes );
				Optional<Class<?>> closestExclude = findClosestClass( typedIdentifier.javaClass(), excludes );

				if ( closestInclude.isPresent() && closestExclude.isPresent() ) {
					// if include is a subclass of exclude - then include is more specific, and we allow indexing:
					if ( closestExclude.get().isAssignableFrom( closestInclude.get() ) ) {
						allIncludes.add( typedIdentifier );
					}
					else {
						allExcludes.add( typedIdentifier );
					}
				}
				else if ( closestExclude.isPresent() ) {
					allExcludes.add( typedIdentifier );
				}
				else if ( closestInclude.isPresent() ) {
					allIncludes.add( typedIdentifier );
				}
				else {
					// if we don't find either include or exclude then we will defer the decision to either app filter or to a default (include)
					// but that will happen in the filter.
					allTypesProcessed = false;
				}
			}
		}

		return HibernateOrmAutomaticIndexingTypeFilter.create(
				fallback,
				Collections.unmodifiableSet( allIncludes ),
				Collections.unmodifiableSet( allExcludes ),
				allTypesProcessed
		);
	}

	private Optional<Class<?>> findClosestClass(Class<?> current, Set<PojoRawTypeIdentifier<?>> collection) {
		Class<?> closest = Object.class;
		for ( PojoRawTypeIdentifier<?> identifier : collection ) {
			if ( identifier.javaClass().isAssignableFrom( current ) ) {
				closest = identifier.javaClass().isAssignableFrom( closest ) ? closest : identifier.javaClass();
			}
		}

		if ( Object.class.equals( closest ) ) {
			return Optional.empty();
		}
		else {
			return Optional.of( closest );
		}
	}

	private boolean addIfNotPresentInOther(PojoRawTypeIdentifier<?> typeIdentifier, Set<PojoRawTypeIdentifier<?>> a,
			Set<PojoRawTypeIdentifier<?>> b) {
		if ( b.contains( typeIdentifier ) ) {
			throw log.automaticIndexingFilterCannotIncludeExcludeSameType( typeIdentifier, includes, excludes );
		}
		return a.add( typeIdentifier );
	}
}
