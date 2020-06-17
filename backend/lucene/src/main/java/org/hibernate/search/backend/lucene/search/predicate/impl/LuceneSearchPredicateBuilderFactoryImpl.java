/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.List;

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

import org.apache.lucene.search.Query;


public class LuceneSearchPredicateBuilderFactoryImpl implements LuceneSearchPredicateBuilderFactory {

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
		return new LuceneMatchAllPredicateBuilder( searchContext );
	}

	@Override
	public MatchIdPredicateBuilder id() {
		return new LuceneMatchIdPredicateBuilder( searchContext );
	}

	@Override
	public BooleanPredicateBuilder bool() {
		return new LuceneBooleanPredicateBuilder( searchContext );
	}

	@Override
	public MatchPredicateBuilder match(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createMatchPredicateBuilder( searchContext );
	}

	@Override
	public RangePredicateBuilder range(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createRangePredicateBuilder( searchContext );
	}

	@Override
	public PhrasePredicateBuilder phrase(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createPhrasePredicateBuilder( searchContext );
	}

	@Override
	public WildcardPredicateBuilder wildcard(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createWildcardPredicateBuilder( searchContext );
	}

	@Override
	public SimpleQueryStringPredicateBuilder simpleQueryString() {
		return new LuceneSimpleQueryStringPredicateBuilder( searchContext, indexes );
	}

	@Override
	public ExistsPredicateBuilder exists(String absoluteFieldPath) {
		// trying object node first
		LuceneObjectPredicateBuilderFactory objectPredicateBuilderFactory =
				indexes.objectPredicateBuilderFactory( absoluteFieldPath );
		if ( objectPredicateBuilderFactory != null ) {
			return objectPredicateBuilderFactory.createExistsPredicateBuilder( searchContext );
		}

		return indexes.field( absoluteFieldPath ).createExistsPredicateBuilder( searchContext );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder spatialWithinCircle(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createSpatialWithinCirclePredicateBuilder( searchContext );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder spatialWithinPolygon(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createSpatialWithinPolygonPredicateBuilder( searchContext );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder spatialWithinBoundingBox(
			String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createSpatialWithinBoundingBoxPredicateBuilder( searchContext );
	}

	@Override
	public NestedPredicateBuilder nested(String absoluteFieldPath) {
		indexes.checkNestedField( absoluteFieldPath );
		List<String> nestedPathHierarchy = indexes.nestedPathHierarchyForObject( absoluteFieldPath );
		return new LuceneNestedPredicateBuilder( searchContext, absoluteFieldPath, nestedPathHierarchy );
	}

	@Override
	public LuceneSearchPredicateBuilder fromLuceneQuery(Query query) {
		return new LuceneUserProvidedLuceneQueryPredicateBuilder( searchContext, query );
	}
}
