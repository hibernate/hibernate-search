/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.util.impl.common.LoggerFactory;

import org.apache.lucene.search.Query;

/**
 * @author Guillaume Smet
 */
public class SearchPredicateFactoryImpl implements LuceneSearchPredicateFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchTargetModel searchTargetModel;

	public SearchPredicateFactoryImpl(LuceneSearchTargetModel searchTargetModel) {
		this.searchTargetModel = searchTargetModel;
	}

	@Override
	public SearchPredicate toSearchPredicate(LuceneSearchPredicateBuilder builder) {
		return new LuceneSearchPredicate( builder );
	}

	@Override
	public LuceneSearchPredicateBuilder toImplementation(SearchPredicate predicate) {
		if ( !( predicate instanceof LuceneSearchPredicate ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherPredicates( predicate );
		}
		return (LuceneSearchPredicate) predicate;
	}

	@Override
	public void contribute(LuceneSearchPredicateCollector collector,
			LuceneSearchPredicateBuilder builder) {
		collector.collectPredicate( builder.build( LuceneSearchPredicateContext.root() ) );
	}

	@Override
	public MatchAllPredicateBuilder<LuceneSearchPredicateBuilder> matchAll() {
		return new MatchAllPredicateBuilderImpl();
	}

	@Override
	public BooleanJunctionPredicateBuilder<LuceneSearchPredicateBuilder> bool() {
		return new BooleanJunctionPredicateBuilderImpl();
	}

	@Override
	public MatchPredicateBuilder<LuceneSearchPredicateBuilder> match(String absoluteFieldPath) {
		return searchTargetModel.getSchemaNode( absoluteFieldPath ).getPredicateBuilderFactory().createMatchPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public RangePredicateBuilder<LuceneSearchPredicateBuilder> range(String absoluteFieldPath) {
		return searchTargetModel.getSchemaNode( absoluteFieldPath ).getPredicateBuilderFactory().createRangePredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder<LuceneSearchPredicateBuilder> spatialWithinCircle(String absoluteFieldPath) {
		return searchTargetModel.getSchemaNode( absoluteFieldPath ).getPredicateBuilderFactory().createSpatialWithinCirclePredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<LuceneSearchPredicateBuilder> spatialWithinPolygon(String absoluteFieldPath) {
		return searchTargetModel.getSchemaNode( absoluteFieldPath ).getPredicateBuilderFactory().createSpatialWithinPolygonPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<LuceneSearchPredicateBuilder> spatialWithinBoundingBox(
			String absoluteFieldPath) {
		return searchTargetModel.getSchemaNode( absoluteFieldPath ).getPredicateBuilderFactory()
				.createSpatialWithinBoundingBoxPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public NestedPredicateBuilder<LuceneSearchPredicateBuilder> nested(String absoluteFieldPath) {
		searchTargetModel.checkNestedField( absoluteFieldPath );
		return new NestedPredicateBuilderImpl( absoluteFieldPath );
	}

	@Override
	public LuceneSearchPredicateBuilder fromLuceneQuery(Query query) {
		return new UserProvidedLuceneQueryPredicateContributor( query );
	}

}
