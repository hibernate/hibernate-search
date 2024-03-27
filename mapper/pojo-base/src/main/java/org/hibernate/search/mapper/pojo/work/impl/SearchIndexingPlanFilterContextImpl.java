/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeTypeContextProvider;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanFilterContext;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredSearchIndexingPlanFilter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class SearchIndexingPlanFilterContextImpl implements SearchIndexingPlanFilterContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoScopeTypeContextProvider contextProvider;
	private final Set<PojoRawTypeIdentifier<?>> includes = new HashSet<>();
	private final Set<PojoRawTypeIdentifier<?>> excludes = new HashSet<>();

	public SearchIndexingPlanFilterContextImpl(PojoScopeTypeContextProvider contextProvider) {
		this.contextProvider = contextProvider;
	}

	@Override
	public SearchIndexingPlanFilterContext include(String name) {
		addIfNotPresentInOther(
				contextProvider.nonInterfaceSuperTypeIdentifierByEntityName().getOrFail( name ),
				includes,
				excludes
		);
		return this;
	}

	@Override
	public SearchIndexingPlanFilterContext include(Class<?> clazz) {
		addIfNotPresentInOther(
				contextProvider.nonInterfaceSuperTypeIdentifierForClass( clazz ),
				includes,
				excludes
		);
		return this;
	}

	@Override
	public SearchIndexingPlanFilterContext exclude(String name) {
		addIfNotPresentInOther(
				contextProvider.nonInterfaceSuperTypeIdentifierByEntityName().getOrFail( name ),
				excludes,
				includes
		);
		return this;
	}

	@Override
	public SearchIndexingPlanFilterContext exclude(Class<?> clazz) {
		addIfNotPresentInOther(
				contextProvider.nonInterfaceSuperTypeIdentifierForClass( clazz ),
				excludes,
				includes
		);
		return this;
	}

	public ConfiguredSearchIndexingPlanFilter createFilter(ConfiguredSearchIndexingPlanFilter fallback) {
		Set<PojoRawTypeIdentifier<?>> allIncludes = new HashSet<>();
		Set<PojoRawTypeIdentifier<?>> allExcludes = new HashSet<>();
		Set<PojoRawTypeIdentifier<?>> processedByThisContext = new HashSet<>();

		Set<PojoRawTypeIdentifier<?>> allIndexedAndContainedTypes = contextProvider.allIndexedAndContainedTypes();

		// First we want to go through explicit include/exclude rules defined by this context and
		// process the hierarchies:
		for ( PojoRawTypeIdentifier<?> typeIdentifier : contextProvider.allNonInterfaceSuperTypes() ) {
			if ( includes.contains( typeIdentifier ) ) {
				include( allIncludes, allExcludes, processedByThisContext, typeIdentifier );
			}
			else if ( excludes.contains( typeIdentifier ) ) {
				exclude( allIncludes, allExcludes, processedByThisContext, typeIdentifier );
			}
		}

		// After context rules are processed we are going through all the types and since `contextProvider.allNonInterfaceSuperTypes()`
		// includes all possible type identifiers that we care about, we no longer lookup the hierarchies. We only want to
		// look at the fallback rules for the types that were not processed so far and looking up hierarchies at this point
		// might lead to an unnecessary collision.
		// Also at this point we only care about indexed and contained types as we shouldn't get others to be tested
		// by the filter...
		for ( PojoRawTypeIdentifier<?> typeIdentifier : allIndexedAndContainedTypes ) {
			if ( !processedByThisContext.contains( typeIdentifier ) ) {
				if ( fallback == null || fallback.isIncluded( typeIdentifier ) ) {
					allIncludes.add( typeIdentifier );
				}
				else {
					allExcludes.add( typeIdentifier );
				}
			}
		}

		return ConfiguredSearchIndexingPlanFilter.create(
				Collections.unmodifiableSet( allIncludes ),
				Collections.unmodifiableSet( allExcludes )
		);
	}

	private void exclude(Set<PojoRawTypeIdentifier<?>> allIncludes, Set<PojoRawTypeIdentifier<?>> allExcludes,
			Set<PojoRawTypeIdentifier<?>> processed, PojoRawTypeIdentifier<?> typeIdentifier) {
		contextProvider.forNonInterfaceSuperType( typeIdentifier ).forEach( typeContext -> {
			PojoRawTypeIdentifier<?> identifier = typeContext.typeIdentifier();
			allIncludes.remove( identifier );
			allExcludes.add( identifier );
			processed.add( identifier );
		} );
	}

	private void include(Set<PojoRawTypeIdentifier<?>> allIncludes, Set<PojoRawTypeIdentifier<?>> allExcludes,
			Set<PojoRawTypeIdentifier<?>> processed, PojoRawTypeIdentifier<?> typeIdentifier) {
		contextProvider.forNonInterfaceSuperType( typeIdentifier ).forEach( typeContext -> {
			PojoRawTypeIdentifier<?> identifier = typeContext.typeIdentifier();
			allIncludes.add( identifier );
			allExcludes.remove( identifier );
			processed.add( identifier );
		} );
	}

	private boolean addIfNotPresentInOther(PojoRawTypeIdentifier<?> typeIdentifier, Set<PojoRawTypeIdentifier<?>> a,
			Set<PojoRawTypeIdentifier<?>> b) {
		if ( b.contains( typeIdentifier ) ) {
			throw log.indexingPlanFilterCannotIncludeExcludeSameType( typeIdentifier, includes, excludes );
		}
		return a.add( typeIdentifier );
	}
}
