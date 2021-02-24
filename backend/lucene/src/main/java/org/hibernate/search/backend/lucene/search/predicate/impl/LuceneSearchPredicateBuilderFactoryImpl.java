/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexesContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchObjectFieldContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNamedPredicateNode;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;


public class LuceneSearchPredicateBuilderFactoryImpl implements LuceneSearchPredicateBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchContext searchContext;
	private final LuceneSearchIndexesContext indexes;

	public LuceneSearchPredicateBuilderFactoryImpl(LuceneSearchContext searchContext) {
		this.searchContext = searchContext;
		this.indexes = searchContext.indexes();
	}

	@Override
	public void contribute(LuceneSearchPredicateCollector collector, SearchPredicate predicate) {
		LuceneSearchPredicate lucenePredicate = LuceneSearchPredicate.from( searchContext, predicate );
		collector.collectPredicate( lucenePredicate.toQuery( PredicateRequestContext.root() ) );
	}

	@Override
	public MatchAllPredicateBuilder matchAll() {
		return new LuceneMatchAllPredicate.Builder( searchContext );
	}

	@Override
	public MatchIdPredicateBuilder id() {
		return new LuceneMatchIdPredicate.Builder( searchContext );
	}

	@Override
	public BooleanPredicateBuilder bool() {
		return new LuceneBooleanPredicate.Builder( searchContext );
	}

	@Override
	public MatchPredicateBuilder match(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.MATCH, searchContext );
	}

	@Override
	public RangePredicateBuilder range(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.RANGE, searchContext );
	}

	@Override
	public PhrasePredicateBuilder phrase(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.PHRASE, searchContext );
	}

	@Override
	public NamedPredicateBuilder named(SearchPredicateFactory namedPredicateFactory, String name) {
		LuceneIndexSchemaNamedPredicateNode namedPredicate = indexes.namedPredicate( name );
		return new LuceneNamedPredicate.Builder( searchContext, namedPredicateFactory, namedPredicate );
	}

	@Override
	public WildcardPredicateBuilder wildcard(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.WILDCARD, searchContext );
	}

	@Override
	public SimpleQueryStringPredicateBuilder simpleQueryString() {
		return new LuceneSimpleQueryStringPredicate.Builder( searchContext, indexes );
	}

	@Override
	public ExistsPredicateBuilder exists(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.EXISTS, searchContext );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder spatialWithinCircle(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath )
				.queryElement( PredicateTypeKeys.SPATIAL_WITHIN_CIRCLE, searchContext );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder spatialWithinPolygon(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath )
				.queryElement( PredicateTypeKeys.SPATIAL_WITHIN_POLYGON, searchContext );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder spatialWithinBoundingBox(
			String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath )
				.queryElement( PredicateTypeKeys.SPATIAL_WITHIN_BOUNDING_BOX, searchContext );
	}

	@Override
	public NestedPredicateBuilder nested(String absoluteFieldPath) {
		LuceneSearchObjectFieldContext field = indexes.field( absoluteFieldPath ).toObjectField();
		if ( !field.nested() ) {
			throw log.nonNestedFieldForNestedQuery( absoluteFieldPath,
					EventContexts.fromIndexNames( indexes.indexNames() ) );
		}
		return new LuceneNestedPredicate.Builder( searchContext, absoluteFieldPath,
				field.nestedPathHierarchy() );
	}

	@Override
	public LuceneSearchPredicate fromLuceneQuery(Query query) {
		return new LuceneUserProvidedLuceneQueryPredicate( searchContext, query );
	}
}
