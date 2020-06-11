/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexesContext;
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

import com.google.gson.JsonObject;

public class ElasticsearchSearchPredicateBuilderFactoryImpl implements ElasticsearchSearchPredicateBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchSearchContext searchContext;
	private final ElasticsearchSearchIndexesContext indexes;

	public ElasticsearchSearchPredicateBuilderFactoryImpl(ElasticsearchSearchContext searchContext) {
		this.searchContext = searchContext;
		this.indexes = searchContext.indexes();
	}

	@Override
	public SearchPredicate toSearchPredicate(ElasticsearchSearchPredicateBuilder builder) {
		return new ElasticsearchSearchPredicate( builder, indexes.hibernateSearchIndexNames() );
	}

	@Override
	public ElasticsearchSearchPredicateBuilder toImplementation(SearchPredicate predicate) {
		if ( !( predicate instanceof ElasticsearchSearchPredicate ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherPredicates( predicate );
		}

		ElasticsearchSearchPredicate casted = (ElasticsearchSearchPredicate) predicate;
		if ( !indexes.hibernateSearchIndexNames().equals( casted.getIndexNames() ) ) {
			throw log.predicateDefinedOnDifferentIndexes( predicate, casted.getIndexNames(),
					indexes.hibernateSearchIndexNames() );
		}
		return casted;
	}

	@Override
	public void contribute(ElasticsearchSearchPredicateCollector collector,
			ElasticsearchSearchPredicateBuilder builder) {
		collector.collectPredicate( builder.build( collector.getRootPredicateContext() ) );
	}

	@Override
	public MatchAllPredicateBuilder<ElasticsearchSearchPredicateBuilder> matchAll() {
		return new ElasticsearchMatchAllPredicateBuilder();
	}

	@Override
	public MatchIdPredicateBuilder<ElasticsearchSearchPredicateBuilder> id() {
		return new ElasticsearchMatchIdPredicateBuilder( searchContext );
	}

	@Override
	public BooleanPredicateBuilder<ElasticsearchSearchPredicateBuilder> bool() {
		return new ElasticsearchBooleanPredicateBuilder();
	}

	@Override
	public MatchPredicateBuilder<ElasticsearchSearchPredicateBuilder> match(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createMatchPredicateBuilder( searchContext );
	}

	@Override
	public RangePredicateBuilder<ElasticsearchSearchPredicateBuilder> range(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createRangePredicateBuilder( searchContext );
	}

	@Override
	public PhrasePredicateBuilder<ElasticsearchSearchPredicateBuilder> phrase(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createPhrasePredicateBuilder();
	}

	@Override
	public WildcardPredicateBuilder<ElasticsearchSearchPredicateBuilder> wildcard(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createWildcardPredicateBuilder();
	}

	@Override
	public SimpleQueryStringPredicateBuilder<ElasticsearchSearchPredicateBuilder> simpleQueryString() {
		return new ElasticsearchSimpleQueryStringPredicateBuilder( indexes );
	}

	@Override
	public ExistsPredicateBuilder<ElasticsearchSearchPredicateBuilder> exists(String absoluteFieldPath) {
		List<String> nestedPathHierarchy;
		if ( indexes.hasSchemaObjectNodeComponent( absoluteFieldPath ) ) {
			nestedPathHierarchy = indexes.nestedPathHierarchyForObject( absoluteFieldPath );
		}
		else {
			ElasticsearchSearchFieldContext<?> field = indexes.field( absoluteFieldPath );
			nestedPathHierarchy = field.nestedPathHierarchy();
			// Make sure to fail for fields with different type
			// We may be able to relax this constraint, but that would require more extensive testing
			field.type().predicateBuilderFactory();
		}
		return new ElasticsearchExistsPredicateBuilder( absoluteFieldPath, nestedPathHierarchy );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder<ElasticsearchSearchPredicateBuilder> spatialWithinCircle(
			String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createSpatialWithinCirclePredicateBuilder();
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<ElasticsearchSearchPredicateBuilder> spatialWithinPolygon(
			String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createSpatialWithinPolygonPredicateBuilder();
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<ElasticsearchSearchPredicateBuilder> spatialWithinBoundingBox(
			String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createSpatialWithinBoundingBoxPredicateBuilder();
	}

	@Override
	public NestedPredicateBuilder<ElasticsearchSearchPredicateBuilder> nested(String absoluteFieldPath) {
		indexes.checkNestedField( absoluteFieldPath );
		List<String> nestedPathHierarchy = indexes.nestedPathHierarchyForObject( absoluteFieldPath );
		return new ElasticsearchNestedPredicateBuilder( absoluteFieldPath, nestedPathHierarchy );
	}

	@Override
	public ElasticsearchSearchPredicateBuilder fromJson(JsonObject jsonObject) {
		return new ElasticsearchUserProvidedJsonPredicateBuilder( jsonObject );
	}

	@Override
	public ElasticsearchSearchPredicateBuilder fromJson(String jsonString) {
		return fromJson( searchContext.userFacingGson().fromJson( jsonString, JsonObject.class ) );
	}
}
