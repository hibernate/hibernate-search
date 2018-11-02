/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import java.util.function.Function;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchTarget;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;

/**
 * A wrapper around {@link MappedIndexSearchTarget} providing some syntactic sugar,
 * such as methods that do not force to provide a session context.
 */
public class StubMappingSearchTarget {

	private final MappedIndexSearchTarget indexSearchTarget;

	StubMappingSearchTarget(MappedIndexSearchTarget indexSearchTarget) {
		this.indexSearchTarget = indexSearchTarget;
	}

	public SearchQueryResultDefinitionContext<DocumentReference, DocumentReference> query() {
		return query( new StubSessionContext() );
	}

	public <R, O> SearchQueryResultDefinitionContext<R, O> query(
			Function<DocumentReference, R> documentReferenceTransformer, ObjectLoader<R, O> objectLoader) {
		return query( new StubSessionContext(), documentReferenceTransformer, objectLoader );
	}

	public SearchQueryResultDefinitionContext<DocumentReference, DocumentReference> query(StubSessionContext sessionContext) {
		return indexSearchTarget.query( sessionContext );
	}

	public <R, O> SearchQueryResultDefinitionContext<R, O> query(StubSessionContext sessionContext,
			Function<DocumentReference, R> documentReferenceTransformer, ObjectLoader<R, O> objectLoader) {
		return indexSearchTarget.query( sessionContext, documentReferenceTransformer, objectLoader );
	}

	public SearchPredicateFactoryContext predicate() {
		return indexSearchTarget.predicate();
	}

	public SearchSortContainerContext sort() {
		return indexSearchTarget.sort();
	}

	public SearchProjectionFactoryContext projection() {
		return indexSearchTarget.projection();
	}
}
