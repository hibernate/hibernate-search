/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexesContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchCompositeIndexSchemaElementContext;
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
import org.hibernate.search.engine.search.predicate.spi.RegexpPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.TermsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNamedPredicateNode;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;

public class ElasticsearchSearchPredicateBuilderFactoryImpl implements ElasticsearchSearchPredicateBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
	public WildcardPredicateBuilder wildcard(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.WILDCARD, searchContext );
	}

	@Override
	public RegexpPredicateBuilder regexp(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.REGEXP, searchContext );
	}

	@Override
	public TermsPredicateBuilder terms(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.TERMS, searchContext );
	}

	@Override
	public SimpleQueryStringPredicateBuilder simpleQueryString() {
		return new ElasticsearchSimpleQueryStringPredicate.Builder( searchContext );
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
	public SpatialWithinBoundingBoxPredicateBuilder spatialWithinBoundingBox(String absoluteFieldPath) {
		return indexes.field( absoluteFieldPath )
				.queryElement( PredicateTypeKeys.SPATIAL_WITHIN_BOUNDING_BOX, searchContext );
	}

	@Override
	public NestedPredicateBuilder nested(String absoluteFieldPath) {
		ElasticsearchSearchCompositeIndexSchemaElementContext field = indexes.field( absoluteFieldPath ).toObjectField();
		if ( !field.nested() ) {
			throw log.nonNestedFieldForNestedQuery( absoluteFieldPath,
					EventContexts.fromIndexNames( indexes.hibernateSearchIndexNames() ) );
		}
		return new ElasticsearchNestedPredicate.Builder( searchContext, absoluteFieldPath,
				field.nestedPathHierarchy() );
	}

	@Override
	public ElasticsearchSearchPredicate fromJson(JsonObject jsonObject) {
		return new ElasticsearchUserProvidedJsonPredicate( searchContext, jsonObject );
	}

	@Override
	public ElasticsearchSearchPredicate fromJson(String jsonString) {
		return fromJson( searchContext.userFacingGson().fromJson( jsonString, JsonObject.class ) );
	}

	@Override
	public NamedPredicateBuilder named(SearchPredicateFactory predicateFactory, String name) {
		ElasticsearchIndexSchemaNamedPredicateNode namedPredicate = indexes.namedPredicate( name );
		return new ElasticsearchNamedPredicate.Builder( searchContext, predicateFactory, namedPredicate );
	}
}
