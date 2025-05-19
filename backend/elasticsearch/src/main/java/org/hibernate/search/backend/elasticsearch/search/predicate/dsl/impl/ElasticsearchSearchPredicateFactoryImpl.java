/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.dsl.impl;

import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateIndexScope;
import org.hibernate.search.engine.search.common.NonStaticMetamodelScope;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.dsl.spi.StaticPredicateFinalStep;

import com.google.gson.JsonObject;

public class ElasticsearchSearchPredicateFactoryImpl<SR>
		extends AbstractSearchPredicateFactory<
				SR,
				ElasticsearchSearchPredicateFactory<SR>,
				ElasticsearchSearchPredicateIndexScope<?>>
		implements ElasticsearchSearchPredicateFactory<SR> {

	public ElasticsearchSearchPredicateFactoryImpl(
			Class<SR> scopeRootType,
			SearchPredicateDslContext<ElasticsearchSearchPredicateIndexScope<?>> dslContext) {
		super( scopeRootType, dslContext );
	}

	@Override
	public ElasticsearchSearchPredicateFactory<SR> withRoot(String objectFieldPath) {
		return new ElasticsearchSearchPredicateFactoryImpl<>( scopeRootType, dslContext.rescope(
				dslContext.scope().withRoot( objectFieldPath ) ) );
	}

	@SuppressWarnings("unchecked") // well because we check ;)
	@Override
	public <SR2> ElasticsearchSearchPredicateFactory<SR2> withScopeRoot(Class<SR2> scopeRootType) {
		if ( this.scopeRootType.equals( scopeRootType ) ) {
			return (ElasticsearchSearchPredicateFactory<SR2>) this;
		}
		if (
			// if we want the "untyped" version of the factory we can get it from any other factory
		// e.g. we have one tied to a Book__ and we want to use some "raw" string paths in a named predicate.
		scopeRootType.equals( NonStaticMetamodelScope.class )
				// scope type is in the same hierarchy:
				|| this.scopeRootType.isAssignableFrom( scopeRootType )
				|| scopeRootType.isAssignableFrom( this.scopeRootType )
		) {
			return new ElasticsearchSearchPredicateFactoryImpl<>( scopeRootType, dslContext );
		}
		throw QueryLog.INSTANCE.incompatibleScopeRootType( scopeRootType, this.scopeRootType );
	}

	@Override
	public PredicateFinalStep fromJson(String jsonString) {
		return new StaticPredicateFinalStep( dslContext.scope().predicateBuilders().fromJson( jsonString ) );
	}

	@Override
	public PredicateFinalStep fromJson(JsonObject jsonObject) {
		return new StaticPredicateFinalStep( dslContext.scope().predicateBuilders().fromJson( jsonObject ) );
	}
}
