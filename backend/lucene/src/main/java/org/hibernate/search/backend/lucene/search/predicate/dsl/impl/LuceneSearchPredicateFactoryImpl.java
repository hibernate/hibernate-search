/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.dsl.impl;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateIndexScope;
import org.hibernate.search.engine.search.common.NonStaticMetamodelScope;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.dsl.spi.StaticPredicateFinalStep;

import org.apache.lucene.search.Query;

public class LuceneSearchPredicateFactoryImpl<SR>
		extends AbstractSearchPredicateFactory<
				SR,
				LuceneSearchPredicateFactory<SR>,
				LuceneSearchPredicateIndexScope<?>>
		implements LuceneSearchPredicateFactory<SR> {

	public LuceneSearchPredicateFactoryImpl(Class<SR> scopeRootType,
			SearchPredicateDslContext<LuceneSearchPredicateIndexScope<?>> dslContext) {
		super( scopeRootType, dslContext );
	}

	@Override
	public LuceneSearchPredicateFactory<SR> withRoot(String objectFieldPath) {
		return new LuceneSearchPredicateFactoryImpl<>( scopeRootType, dslContext.rescope(
				dslContext.scope().withRoot( objectFieldPath ) ) );
	}

	@Override
	public PredicateFinalStep fromLuceneQuery(Query luceneQuery) {
		return new StaticPredicateFinalStep( dslContext.scope().predicateBuilders().fromLuceneQuery( luceneQuery ) );
	}

	@SuppressWarnings("unchecked") // well because we check ;)
	@Override
	public <SR2> LuceneSearchPredicateFactory<SR2> withScopeRoot(Class<SR2> scopeRootType) {
		if ( this.scopeRootType.equals( scopeRootType ) ) {
			return (LuceneSearchPredicateFactory<SR2>) this;
		}
		if (
			// if we want the "untyped" version of the factory we can get it from any other factory
		// e.g. we have one tied to a Book__ and we want to use some "raw" string paths in a named predicate.
		scopeRootType.equals( NonStaticMetamodelScope.class )
				// scope type is in the same hierarchy:
				|| this.scopeRootType.isAssignableFrom( scopeRootType )
				|| scopeRootType.isAssignableFrom( this.scopeRootType )
		) {
			return new LuceneSearchPredicateFactoryImpl<>( scopeRootType, dslContext );
		}
		throw QueryLog.INSTANCE.incompatibleScopeRootType( scopeRootType, this.scopeRootType );
	}
}
