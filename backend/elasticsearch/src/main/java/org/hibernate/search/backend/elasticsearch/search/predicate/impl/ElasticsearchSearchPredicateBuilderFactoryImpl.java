/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.List;

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

import com.google.gson.JsonObject;

public class ElasticsearchSearchPredicateBuilderFactoryImpl implements ElasticsearchSearchPredicateBuilderFactory {

	private final ElasticsearchSearchContext searchContext;
	private final ElasticsearchSearchIndexesContext indexes;

	public ElasticsearchSearchPredicateBuilderFactoryImpl(ElasticsearchSearchContext searchContext) {
		this.searchContext = searchContext;
		this.indexes = searchContext.indexes();
	}

	@Override
	public void contribute(ElasticsearchSearchPredicateCollector collector, SearchPredicate predicate) {
		ElasticsearchSearchPredicate lucenePredicate = ElasticsearchSearchPredicate.from( searchContext, predicate );
		collector.collectPredicate( lucenePredicate.toJsonQuery( collector.getRootPredicateContext() ) );
	}

	@Override
	public MatchAllPredicateBuilder matchAll() {
		return new ElasticsearchMatchAllPredicate.Builder( searchContext );
	}

	@Override
	public MatchIdPredicateBuilder id() {
		return new ElasticsearchMatchIdPredicate.Builder( searchContext );
	}

	@Override
	public BooleanPredicateBuilder bool() {
		return new ElasticsearchBooleanPredicate.Builder( searchContext );
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
		return new ElasticsearchSimpleQueryStringPredicate.Builder( searchContext );
	}

	@Override
	public ExistsPredicateBuilder exists(String absoluteFieldPath) {
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
		return new ElasticsearchExistsPredicate.Builder( searchContext, absoluteFieldPath, nestedPathHierarchy );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder spatialWithinCircle(
			String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).createSpatialWithinCirclePredicateBuilder( searchContext );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder spatialWithinPolygon(
			String absoluteFieldPath) {
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
		return new ElasticsearchNestedPredicate.Builder( searchContext, absoluteFieldPath, nestedPathHierarchy );
	}

	@Override
	public ElasticsearchSearchPredicate fromJson(JsonObject jsonObject) {
		return new ElasticsearchUserProvidedJsonPredicate( searchContext, jsonObject );
	}

	@Override
	public ElasticsearchSearchPredicate fromJson(String jsonString) {
		return fromJson( searchContext.userFacingGson().fromJson( jsonString, JsonObject.class ) );
	}
}
