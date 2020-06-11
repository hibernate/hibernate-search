/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexesContext;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneObjectPredicateBuilderFactory;
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


public class LuceneSearchPredicateBuilderFactoryImpl implements LuceneSearchPredicateBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchContext searchContext;
	private final LuceneSearchIndexesContext indexes;

	public LuceneSearchPredicateBuilderFactoryImpl(LuceneSearchContext searchContext) {
		this.searchContext = searchContext;
		this.indexes = searchContext.indexes();
	}

	@Override
	public SearchPredicate toSearchPredicate(LuceneSearchPredicateBuilder builder) {
		return new LuceneSearchPredicate( indexes.indexNames(), builder );
	}

	@Override
	public LuceneSearchPredicateBuilder toImplementation(SearchPredicate predicate) {
		if ( !( predicate instanceof LuceneSearchPredicate ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherPredicates( predicate );
		}
		LuceneSearchPredicate casted = (LuceneSearchPredicate) predicate;
		if ( !indexes.indexNames().equals( casted.getIndexNames() ) ) {
			throw log.predicateDefinedOnDifferentIndexes( predicate, casted.getIndexNames(), indexes.indexNames() );
		}
		return casted;
	}

	@Override
	public void contribute(LuceneSearchPredicateCollector collector,
			LuceneSearchPredicateBuilder builder) {
		collector.collectPredicate( builder.build( LuceneSearchPredicateContext.root() ) );
	}

	@Override
	public MatchAllPredicateBuilder<LuceneSearchPredicateBuilder> matchAll() {
		return new LuceneMatchAllPredicateBuilder();
	}

	@Override
	public MatchIdPredicateBuilder<LuceneSearchPredicateBuilder> id() {
		return new LuceneMatchIdPredicateBuilder( searchContext );
	}

	@Override
	public BooleanPredicateBuilder<LuceneSearchPredicateBuilder> bool() {
		return new LuceneBooleanPredicateBuilder();
	}

	@Override
	public MatchPredicateBuilder<LuceneSearchPredicateBuilder> match(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createMatchPredicateBuilder( searchContext );
	}

	@Override
	public RangePredicateBuilder<LuceneSearchPredicateBuilder> range(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createRangePredicateBuilder( searchContext );
	}

	@Override
	public PhrasePredicateBuilder<LuceneSearchPredicateBuilder> phrase(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createPhrasePredicateBuilder( searchContext );
	}

	@Override
	public WildcardPredicateBuilder<LuceneSearchPredicateBuilder> wildcard(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createWildcardPredicateBuilder();
	}

	@Override
	public SimpleQueryStringPredicateBuilder<LuceneSearchPredicateBuilder> simpleQueryString() {
		return new LuceneSimpleQueryStringPredicateBuilder( searchContext, indexes );
	}

	@Override
	public ExistsPredicateBuilder<LuceneSearchPredicateBuilder> exists(String absoluteFieldPath) {
		// trying object node first
		LuceneObjectPredicateBuilderFactory objectPredicateBuilderFactory =
				indexes.objectPredicateBuilderFactory( absoluteFieldPath );
		if ( objectPredicateBuilderFactory != null ) {
			return objectPredicateBuilderFactory.createExistsPredicateBuilder();
		}

		return indexes.field( absoluteFieldPath ).createExistsPredicateBuilder();
	}

	@Override
	public SpatialWithinCirclePredicateBuilder<LuceneSearchPredicateBuilder> spatialWithinCircle(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createSpatialWithinCirclePredicateBuilder();
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<LuceneSearchPredicateBuilder> spatialWithinPolygon(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createSpatialWithinPolygonPredicateBuilder();
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<LuceneSearchPredicateBuilder> spatialWithinBoundingBox(
			String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createSpatialWithinBoundingBoxPredicateBuilder();
	}

	@Override
	public NestedPredicateBuilder<LuceneSearchPredicateBuilder> nested(String absoluteFieldPath) {
		indexes.checkNestedField( absoluteFieldPath );
		List<String> nestedPathHierarchy = indexes.nestedPathHierarchyForObject( absoluteFieldPath );
		return new LuceneNestedPredicateBuilder( absoluteFieldPath, nestedPathHierarchy );
	}

	@Override
	public LuceneSearchPredicateBuilder fromLuceneQuery(Query query) {
		return new LuceneUserProvidedLuceneQueryPredicateBuilder( query );
	}
}
