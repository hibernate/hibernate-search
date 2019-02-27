/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.spi.DslConverter;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class LuceneGeoPointFieldPredicateBuilderFactory implements LuceneFieldPredicateBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final LuceneGeoPointFieldPredicateBuilderFactory INSTANCE = new LuceneGeoPointFieldPredicateBuilderFactory();

	private LuceneGeoPointFieldPredicateBuilderFactory() {
	}

	@Override
	public boolean isDslCompatibleWith(LuceneFieldPredicateBuilderFactory other, DslConverter dslConverter) {
		return INSTANCE == other;
	}

	@Override
	public MatchPredicateBuilder<LuceneSearchPredicateBuilder> createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath, DslConverter dslConverter) {
		throw log.matchPredicatesNotSupportedByGeoPoint(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public RangePredicateBuilder<LuceneSearchPredicateBuilder> createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath, DslConverter dslConverter) {
		throw log.rangePredicatesNotSupportedByGeoPoint(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public PhrasePredicateBuilder<LuceneSearchPredicateBuilder> createPhrasePredicateBuilder(String absoluteFieldPath) {
		throw log.textPredicatesNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public LuceneSimpleQueryStringPredicateBuilderFieldContext createSimpleQueryStringFieldContext(
			String absoluteFieldPath) {
		throw log.textPredicatesNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public SpatialWithinCirclePredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinCirclePredicateBuilder(
			String absoluteFieldPath) {
		return new LuceneGeoPointSpatialWithinCirclePredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinPolygonPredicateBuilder(
			String absoluteFieldPath) {
		return new LuceneGeoPointSpatialWithinPolygonPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinBoundingBoxPredicateBuilder(
			String absoluteFieldPath) {
		return new LuceneGeoPointSpatialWithinBoundingBoxPredicateBuilder( absoluteFieldPath );
	}
}
