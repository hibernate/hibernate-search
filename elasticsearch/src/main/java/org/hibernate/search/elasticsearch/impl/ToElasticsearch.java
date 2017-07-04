/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;
import org.hibernate.search.backend.spi.DeletionQuery;
import org.hibernate.search.elasticsearch.impl.JsonBuilder.JsonAppender;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.filter.impl.CachingWrapperQuery;
import org.hibernate.search.query.dsl.impl.DiscreteFacetRequest;
import org.hibernate.search.query.dsl.impl.FacetRange;
import org.hibernate.search.query.dsl.impl.RangeFacetRequest;
import org.hibernate.search.query.dsl.impl.RemoteMatchQuery;
import org.hibernate.search.query.dsl.impl.RemotePhraseQuery;
import org.hibernate.search.query.dsl.impl.RemoteSimpleQueryStringQuery;
import org.hibernate.search.query.dsl.impl.RemoteSimpleQueryStringQuery.Field;
import org.hibernate.search.query.dsl.sort.impl.NativeSortField;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.DistanceSortField;
import org.hibernate.search.spatial.impl.DistanceQuery;
import org.hibernate.search.spatial.impl.SpatialHashQuery;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * Various utilities to transform Hibernate Search API into Elasticsearch JSON.
 *
 * @author Guillaume Smet
 * @author Gunnar Morling
 */
public class ToElasticsearch {

	/*
	 * A specific suffix for facet fields that avoids conflicts with existing names.
	 */
	public static final String FACET_FIELD_SUFFIX = "__HSearch_Facet";

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final int DEFAULT_SLOP = 0;
	private static final int DEFAULT_MAX_EDIT_DISTANCE = 0;
	private static final float DEFAULT_BOOST = 1.0f;
	private static final String BOOST_OPERATOR = "^";

	private static final JsonParser JSON_PARSER = new JsonParser();

	private static final JsonPrimitive SORT_ORDER_ASC = new JsonPrimitive( "asc" );
	private static final JsonPrimitive SORT_ORDER_DESC = new JsonPrimitive( "desc" );
	private static final JsonPrimitive SORT_MISSING_LAST = new JsonPrimitive( "_last" );
	private static final JsonPrimitive SORT_MISSING_FIRST = new JsonPrimitive( "_first" );

	private static final Map<SortField.Type, Number> SORT_FIELD_SCALAR_MINIMUMS = new EnumMap<>( SortField.Type.class );
	private static final Map<SortField.Type, Number> SORT_FIELD_SCALAR_DEFAULTS = new EnumMap<>( SortField.Type.class );
	private static final Map<SortField.Type, Number> SORT_FIELD_SCALAR_MAXIMUMS = new EnumMap<>( SortField.Type.class );
	private static final Map<SortField.Type, Class<? extends Number>> SORT_FIELD_SCALAR_TYPES = new EnumMap<>( SortField.Type.class );
	static {
		initSortFieldScalarValues( SortField.Type.DOUBLE, Double.class, Double.MIN_VALUE, 0.0d, Double.MAX_VALUE );
		initSortFieldScalarValues( SortField.Type.FLOAT, Float.class, Float.MIN_VALUE, 0.0f, Float.MAX_VALUE );
		initSortFieldScalarValues( SortField.Type.LONG, Long.class, Long.MIN_VALUE, 0L, Long.MAX_VALUE );
		initSortFieldScalarValues( SortField.Type.INT, Integer.class, Integer.MIN_VALUE, 0, Integer.MAX_VALUE );
	}

	private static void initSortFieldScalarValues(Type type, Class<? extends Number> clazz,
			Number minValue, Number defaultValue, Number maxValue) {
		SORT_FIELD_SCALAR_MINIMUMS.put( type, minValue );
		SORT_FIELD_SCALAR_DEFAULTS.put( type, defaultValue );
		SORT_FIELD_SCALAR_MAXIMUMS.put( type, maxValue );
		SORT_FIELD_SCALAR_TYPES.put( type, clazz );
	}

	private ToElasticsearch() {
	}

	public static void addFacetingRequest(JsonBuilder.Object jsonQuery, FacetingRequest facetingRequest,
			String sourceFieldAbsoluteName, String facetRelativeName) {
		String aggregationFieldName = sourceFieldAbsoluteName + "." + facetRelativeName + FACET_FIELD_SUFFIX;

		if ( facetingRequest instanceof DiscreteFacetRequest ) {
			JsonObject termsJsonQuery = JsonBuilder.object().add( "terms",
					JsonBuilder.object()
							.addProperty( "field", aggregationFieldName )
							.addProperty( "size", facetingRequest.getMaxNumberOfFacets() == -1 ? Integer.MAX_VALUE : facetingRequest.getMaxNumberOfFacets() )
							.add( "order", fromFacetSortOrder( facetingRequest.getSort() ) )
							.addProperty( "min_doc_count", facetingRequest.hasZeroCountsIncluded() ? 0 : 1 )
					).build();

			if ( isNested( sourceFieldAbsoluteName ) ) {
				JsonBuilder.Object facetJsonQuery = JsonBuilder.object();
				facetJsonQuery.add( "nested", JsonBuilder.object()
								.addProperty( "path", FieldHelper.getEmbeddedFieldPath( sourceFieldAbsoluteName ) + "." + facetRelativeName + FACET_FIELD_SUFFIX ) );
				facetJsonQuery.add( "aggregations", JsonBuilder.object().add( aggregationFieldName, termsJsonQuery ) );
				jsonQuery.add( facetingRequest.getFacetingName(), facetJsonQuery );
			}
			else {
				jsonQuery.add( facetingRequest.getFacetingName(), termsJsonQuery );
			}
		}
		else if ( facetingRequest instanceof RangeFacetRequest<?> ) {
			RangeFacetRequest<?> rangeFacetingRequest = (RangeFacetRequest<?>) facetingRequest;
			for ( FacetRange<?> facetRange : rangeFacetingRequest.getFacetRangeList() ) {
				JsonBuilder.Object comparisonFragment = JsonBuilder.object();
				if ( facetRange.getMin() != null ) {
					comparisonFragment.addProperty( facetRange.isMinIncluded() ? "gte" : "gt", facetRange.getMin() );
				}
				if ( facetRange.getMax() != null ) {
					comparisonFragment.addProperty( facetRange.isMaxIncluded() ? "lte" : "lt", facetRange.getMax() );
				}

				JsonObject rangeQuery = wrapQueryForNestedIfRequired( aggregationFieldName,
						JsonBuilder.object().add( "range",
								JsonBuilder.object().add( aggregationFieldName, comparisonFragment ) ).build() );

				jsonQuery.add( facetingRequest.getFacetingName() + "-" + facetRange.getIdentifier(),
						JsonBuilder.object().add( "filter", rangeQuery ) );
			}
		}
		else {
			throw LOG.facetingRequestHasUnsupportedType( facetingRequest.getClass().getName() );
		}
	}

	private static JsonObject fromFacetSortOrder(FacetSortOrder sortOrder) {
		JsonObject sort = new JsonObject();
		switch ( sortOrder ) {
			case COUNT_ASC:
				sort.addProperty( "_count", "asc" );
				break;
			case COUNT_DESC:
				sort.addProperty( "_count", "desc" );
				break;
			case FIELD_VALUE:
				sort.addProperty( "_term", "asc" );
				break;
			case RANGE_DEFINITION_ORDER:
				throw LOG.cannotSendRangeDefinitionOrderToElasticsearchBackend();
		}
		return sort;
	}

	public static JsonObject condition(String operator, JsonArray conditions) {
		JsonObject jsonCondition;
		if ( conditions.size() == 1 ) {
			jsonCondition = conditions.get( 0 ).getAsJsonObject();
		}
		else {
			jsonCondition = JsonBuilder.object().add( "bool",
					JsonBuilder.object().add( operator, conditions ) ).build();
		}
		return jsonCondition;
	}

	public static JsonObject fromLuceneQuery(Query query) {
		if ( query instanceof MatchAllDocsQuery ) {
			return convertMatchAllDocsQuery( (MatchAllDocsQuery) query );
		}
		else if ( query instanceof MatchNoDocsQuery ) {
			return convertMatchNoDocsQuery( (MatchNoDocsQuery) query );
		}
		else if ( query instanceof TermQuery ) {
			return convertTermQuery( (TermQuery) query );
		}
		else if ( query instanceof BooleanQuery ) {
			return convertBooleanQuery( (BooleanQuery) query );
		}
		else if ( query instanceof TermRangeQuery ) {
			return convertTermRangeQuery( (TermRangeQuery) query );
		}
		else if ( query instanceof NumericRangeQuery ) {
			return convertNumericRangeQuery( (NumericRangeQuery<?>) query );
		}
		else if ( query instanceof WildcardQuery ) {
			return convertWildcardQuery( (WildcardQuery) query );
		}
		else if ( query instanceof PrefixQuery ) {
			return convertPrefixQuery( (PrefixQuery) query );
		}
		else if ( query instanceof FuzzyQuery ) {
			return convertFuzzyQuery( (FuzzyQuery) query );
		}
		else if ( query instanceof RemotePhraseQuery ) {
			return convertRemotePhraseQuery( (RemotePhraseQuery) query );
		}
		else if ( query instanceof RemoteMatchQuery ) {
			return convertRemoteMatchQuery( (RemoteMatchQuery) query );
		}
		else if ( query instanceof RemoteSimpleQueryStringQuery ) {
			return convertRemoteSimpleQueryStringQuery( (RemoteSimpleQueryStringQuery) query );
		}
		else if ( query instanceof ConstantScoreQuery ) {
			return convertConstantScoreQuery( (ConstantScoreQuery) query );
		}
		else if ( query instanceof FilteredQuery ) {
			return convertFilteredQuery( (FilteredQuery) query );
		}
		else if ( query instanceof QueryWrapperFilter ) {
			JsonObject result = fromLuceneQuery( ( (QueryWrapperFilter) query ).getQuery() );
			return wrapBoostIfNecessary( result, query.getBoost() );
		}
		else if ( query instanceof DistanceQuery ) {
			return convertDistanceQuery( (DistanceQuery) query );
		}
		else if ( query instanceof SpatialHashQuery ) {
			return convertSpatialHashFilter( (SpatialHashQuery) query );
		}
		else if ( query instanceof PhraseQuery ) {
			return convertPhraseQuery( (PhraseQuery) query );
		}
		else if ( query instanceof BoostQuery ) {
			JsonObject result = fromLuceneQuery( ( (BoostQuery) query ).getQuery() );
			return wrapBoostIfNecessary( result, query.getBoost() );
		}
		else if ( query instanceof CachingWrapperQuery ) {
			JsonObject result = fromLuceneQuery( ( (CachingWrapperQuery) query ).getQuery() );
			return wrapBoostIfNecessary( result, query.getBoost() );
		}
		else if ( query instanceof org.apache.lucene.search.CachingWrapperQuery ) {
			JsonObject result = fromLuceneQuery( ( (org.apache.lucene.search.CachingWrapperQuery) query ).getQuery() );
			return wrapBoostIfNecessary( result, query.getBoost() );
		}
		else if ( query instanceof org.apache.lucene.search.CachingWrapperFilter ) {
			JsonObject result = fromLuceneQuery( ( (org.apache.lucene.search.CachingWrapperFilter) query ).getFilter() );
			return wrapBoostIfNecessary( result, query.getBoost() );
		}

		throw LOG.cannotTransformLuceneQueryIntoEsQuery( query );
	}

	public static JsonObject fromDeletionQuery(DocumentBuilderIndexedEntity documentBuilder, DeletionQuery deletionQuery) {
		return fromLuceneQuery( deletionQuery.toLuceneQuery( documentBuilder ) );
	}

	private static JsonObject convertMatchAllDocsQuery(MatchAllDocsQuery matchAllDocsQuery) {
		return JsonBuilder.object().add( "match_all", new JsonObject() ).build();
	}

	private static JsonObject convertMatchNoDocsQuery(MatchNoDocsQuery matchNoDocsQuery) {
		/*
		 * Elasticsearch 2.x does not provide a match_none query, so we work it around
		 * by targeting a type that doesn't exist.
		 * We use a type query, because Elasticsearch 5.x has optimizations that convert
		 * type queries to MatchNoDocsQueries automatically when an unknown type is
		 * requested.
		 */
		return JsonBuilder.object()
				.add( "type", JsonBuilder.object()
						.addProperty( "value", "__HSearch_Workaround_MatchNoDocsQuery" )
				).build();
	}

	private static JsonObject convertBooleanQuery(BooleanQuery booleanQuery) {
		JsonArray musts = new JsonArray();
		JsonArray shoulds = new JsonArray();
		JsonArray mustNots = new JsonArray();
		JsonArray filters = new JsonArray();

		for ( BooleanClause clause : booleanQuery.clauses() ) {
			switch ( clause.getOccur() ) {
				case MUST:
					musts.add( fromLuceneQuery( clause.getQuery() ) );
					break;
				case FILTER:
					filters.add( fromLuceneQuery( clause.getQuery() ) );
					break;
				case MUST_NOT:
					mustNots.add( fromLuceneQuery( clause.getQuery() ) );
					break;
				case SHOULD:
					shoulds.add( fromLuceneQuery( clause.getQuery() ) );
					break;
			}
		}

		JsonBuilder.Object clauses = JsonBuilder.object();

		if ( musts.size() > 1 ) {
			clauses.add( "must", musts );
		}
		else if ( musts.size() == 1 ) {
			clauses.add( "must", musts.iterator().next() );
		}

		if ( shoulds.size() > 1 ) {
			clauses.add( "should", shoulds );
		}
		else if ( shoulds.size() == 1 ) {
			clauses.add( "should", shoulds.iterator().next() );
		}

		if ( mustNots.size() > 1 ) {
			clauses.add( "must_not", mustNots );
		}
		else if ( mustNots.size() == 1 ) {
			clauses.add( "must_not", mustNots.iterator().next() );
		}

		if ( filters.size() > 1 ) {
			clauses.add( "filter", filters );
		}
		else if ( filters.size() == 1 ) {
			clauses.add( "filter", filters.iterator().next() );
		}

		clauses.append( boostAppender( booleanQuery ) );

		JsonObject bool = new JsonObject();
		bool.add( "bool", clauses.build() );
		return bool;
	}

	private static JsonObject convertTermQuery(TermQuery query) {
		String field = query.getTerm().field();

		JsonObject matchQuery = JsonBuilder.object()
				.add( "term",
						JsonBuilder.object().add( field,
								JsonBuilder.object()
										.addProperty( "value", query.getTerm().text() )
										.append( boostAppender( query ) )
						)
				).build();

		return wrapQueryForNestedIfRequired( field, matchQuery );
	}

	private static JsonObject convertWildcardQuery(WildcardQuery query) {
		String field = query.getTerm().field();

		JsonObject wildcardQuery = JsonBuilder.object()
				.add( "wildcard",
						JsonBuilder.object().add( field,
								JsonBuilder.object()
										.addProperty( "value", query.getTerm().text() )
										.append( boostAppender( query ) )
						)
				).build();

		return wrapQueryForNestedIfRequired( field, wildcardQuery );
	}

	private static JsonObject convertPrefixQuery(PrefixQuery query) {
		String field = query.getField();

		JsonObject wildcardQuery = JsonBuilder.object()
				.add( "prefix",
						JsonBuilder.object().add( field,
								JsonBuilder.object()
										.addProperty( "value", query.getPrefix().text() )
										.append( boostAppender( query ) )
						)
				).build();

		return wrapQueryForNestedIfRequired( field, wildcardQuery );
	}

	private static JsonObject convertFuzzyQuery(FuzzyQuery query) {
		String field = query.getTerm().field();

		JsonObject fuzzyQuery = JsonBuilder.object()
				.add( "fuzzy",
						JsonBuilder.object().add( field,
								JsonBuilder.object()
										.addProperty( "value", query.getTerm().text() )
										.addProperty( "fuzziness", query.getMaxEdits() )
										.addProperty( "prefix_length", query.getPrefixLength() )
										.append( boostAppender( query ) )
						)
				).build();

		return wrapQueryForNestedIfRequired( field, fuzzyQuery );
	}

	/**
	 * This is best effort only: the PhraseQuery may contain multiple terms at the same position
	 * (think synonyms) or gaps (think stopwords) and it's in this case impossible to translate
	 * it into a correct ElasticsearchQuery.
	 */
	private static JsonObject convertPhraseQuery(PhraseQuery query) {
		Term[] terms = query.getTerms();

		if ( terms.length == 0 ) {
			throw LOG.cannotQueryOnEmptyPhraseQuery();
		}

		String field = terms[0].field(); // phrase queries are only supporting one field
		StringBuilder phrase = new StringBuilder();
		for ( Term term : terms ) {
			phrase.append( " " ).append( term.text() );
		}

		JsonObject phraseQuery = JsonBuilder.object()
				.add( "match_phrase",
						JsonBuilder.object().add( field,
								JsonBuilder.object()
										.addProperty( "query", phrase.toString().trim() )
										.append( slopAppender( query.getSlop() ) )
										.append( boostAppender( query ) )
								)
				).build();

		return wrapQueryForNestedIfRequired( field, phraseQuery );
	}

	private static JsonObject convertRemotePhraseQuery(RemotePhraseQuery query) {
		if ( StringHelper.isEmpty( query.getPhrase() ) ) {
			throw LOG.cannotQueryOnEmptyPhraseQuery();
		}

		JsonObject phraseQuery = JsonBuilder.object()
				.add( "match_phrase",
						JsonBuilder.object().add( query.getField(),
								JsonBuilder.object()
										.addProperty( "query", query.getPhrase().trim() )
										.append( analyzerAppender(
												query.getOriginalAnalyzerReference(), query.getQueryAnalyzerReference(),
												query.getField()
										) )
										.append( slopAppender( query.getSlop() ) )
										.append( boostAppender( query ) )
								)
				).build();

		return wrapQueryForNestedIfRequired( query.getField(), phraseQuery );
	}

	private static JsonObject convertRemoteMatchQuery(RemoteMatchQuery query) {
		JsonObject matchQuery = JsonBuilder.object()
				.add( "match",
						JsonBuilder.object().add( query.getField(),
								JsonBuilder.object()
										.addProperty( "query", query.getSearchTerms() )
										.append( analyzerAppender(
												query.getOriginalAnalyzerReference(), query.getQueryAnalyzerReference(),
												query.getField()
										) )
										.append( fuzzinessAppender( query.getMaxEditDistance() ) )
										.append( boostAppender( query ) )
						)
				).build();

		return wrapQueryForNestedIfRequired( query.getField(), matchQuery );
	}

	private static JsonObject convertRemoteSimpleQueryStringQuery(RemoteSimpleQueryStringQuery query) {
		JsonBuilder.Object queryBuilder = JsonBuilder.object()
				.addProperty( "query", query.getQuery() )
				.addProperty( "default_operator", query.isMatchAll() ? "and" : "or" );

		Set<String> analyzers = new HashSet<>();
		String overridingRemoteAnalyzerName = null;

		JsonArray fieldArray = new JsonArray();
		for ( Field field : query.getFields() ) {
			StringBuilder sb = new StringBuilder( field.getName() );
			if ( field.getBoost() != DEFAULT_BOOST ) {
				sb.append( BOOST_OPERATOR ).append( field.getBoost() );
			}
			fieldArray.add( sb.toString() );

			String originalRemoteAnalyzerName = query.getOriginalRemoteAnalyzerReference().getAnalyzerName( field.getName() );
			String queryRemoteAnalyzerName = query.getQueryRemoteAnalyzerReference().getAnalyzerName( field.getName() );
			analyzers.add( queryRemoteAnalyzerName );
			if ( !queryRemoteAnalyzerName.equals( originalRemoteAnalyzerName ) ) {
				if ( overridingRemoteAnalyzerName == null ) {
					overridingRemoteAnalyzerName = queryRemoteAnalyzerName;
				}
				else if ( !overridingRemoteAnalyzerName.equals( queryRemoteAnalyzerName ) ) {
					throw LOG.unableToOverrideQueryAnalyzerWithMoreThanOneAnalyzersForSimpleQueryStringQueries(
							Arrays.asList( overridingRemoteAnalyzerName, queryRemoteAnalyzerName ) );
				}
			}
		}
		// we always have at least one field defined
		queryBuilder.add( "fields", fieldArray );

		if ( overridingRemoteAnalyzerName != null ) {
			if ( analyzers.size() == 1 ) {
				queryBuilder.addProperty( "analyzer", overridingRemoteAnalyzerName );
			}
			else {
				throw LOG.unableToOverrideQueryAnalyzerWithMoreThanOneAnalyzersForSimpleQueryStringQueries( analyzers );
			}
		}

		JsonObject simpleQueryStringQuery = JsonBuilder.object()
				.add( "simple_query_string",
						queryBuilder.append( boostAppender( query ) ) )
				.build();

		return simpleQueryStringQuery;
	}

	private static JsonObject convertTermRangeQuery(TermRangeQuery query) {
		JsonBuilder.Object interval = JsonBuilder.object();

		if ( query.getLowerTerm() != null ) {
			interval.addProperty( query.includesLower() ? "gte" : "gt", query.getLowerTerm().utf8ToString() );
		}
		if ( query.getUpperTerm() != null ) {
			interval.addProperty( query.includesUpper() ? "lte" : "lt", query.getUpperTerm().utf8ToString() );
		}

		interval.append( boostAppender( query ) );

		JsonObject range = JsonBuilder.object().add( "range",
						JsonBuilder.object().add( query.getField(), interval ) )
				.build();

		return wrapQueryForNestedIfRequired( query.getField(), range );
	}

	private static JsonObject convertNumericRangeQuery(NumericRangeQuery<?> query) {
		JsonBuilder.Object interval = JsonBuilder.object();

		if ( query.getMin() != null ) {
			interval.addProperty( query.includesMin() ? "gte" : "gt", query.getMin() );
		}
		if ( query.getMax() != null ) {
			interval.addProperty( query.includesMax() ? "lte" : "lt", query.getMax() );
		}

		interval.append( boostAppender( query ) );

		JsonObject range = JsonBuilder.object().add( "range",
						JsonBuilder.object().add( query.getField(), interval ) )
				.build();

		return wrapQueryForNestedIfRequired( query.getField(), range );
	}

	private static JsonObject convertConstantScoreQuery(ConstantScoreQuery query) {
		JsonObject constantScoreQuery = JsonBuilder.object()
				.add( "constant_score",
						JsonBuilder.object()
								.add( "filter", fromLuceneQuery( query.getQuery() ) )
								.append( boostAppender( query ) )
				).build();

		return constantScoreQuery;
	}

	private static JsonObject wrapBoostIfNecessary(JsonObject convertedQuery, float boost) {
		if ( boost != DEFAULT_BOOST ) { // We actually want to use float equality here
			return JsonBuilder.object()
					.add( "bool",
							JsonBuilder.object()
									.add( "must", convertedQuery )
									.addProperty( "boost", boost )
					).build();
		}
		else {
			return convertedQuery;
		}
	}

	private static JsonObject convertFilteredQuery(FilteredQuery query) {
		JsonObject filteredQuery = JsonBuilder.object()
				.add( "bool",
						JsonBuilder.object()
								.add( "must", fromLuceneQuery( query.getQuery() ) )
								.add( "filter", fromLuceneQuery( query.getFilter() ) )
								.append( boostAppender( query ) )
				).build();

		return filteredQuery;
	}

	private static JsonObject convertDistanceQuery(DistanceQuery query) {
		JsonObject distanceQuery = JsonBuilder.object()
				.add( "geo_distance",
						JsonBuilder.object()
								.addProperty( "distance", query.getRadius() + "km" )
								.add( query.getCoordinatesField(),
										JsonBuilder.object()
												.addProperty( "lat", query.getCenter().getLatitude() )
												.addProperty( "lon", query.getCenter().getLongitude() )
								)
				).build();

		distanceQuery = wrapQueryForNestedIfRequired( query.getCoordinatesField(), distanceQuery );

		// we only implement the approximation optimization when we use the hash method as Elasticsearch
		// automatically optimize the geo_distance query with a bounding box filter so we don't need to do it
		// ourselves when we use the range method.
		Query approximationQuery = query.getApproximationQuery();
		if ( approximationQuery instanceof SpatialHashQuery ) {
			distanceQuery = JsonBuilder.object()
					.add( "bool", JsonBuilder.object()
							.add( "must", distanceQuery )
							.add( "filter", convertSpatialHashFilter( (SpatialHashQuery) approximationQuery ) )
					).build();
		}

		return distanceQuery;
	}

	private static JsonObject convertSpatialHashFilter(SpatialHashQuery filter) {
		JsonArray cellsIdsJsonArray = new JsonArray();
		for ( String cellId : filter.getSpatialHashCellsIds() ) {
			cellsIdsJsonArray.add( cellId );
		}

		JsonObject spatialHashFilter = JsonBuilder.object()
				.add( "terms", JsonBuilder.object()
						.add( filter.getFieldName(), cellsIdsJsonArray )
				).build();

		return wrapQueryForNestedIfRequired( filter.getFieldName(), spatialHashFilter );
	}

	private static final JsonBuilder.JsonAppender<Object> NOOP_APPENDER = builder -> { };

	/**
	 * Appender that adds an "analyzer" property if necessary.
	 */
	private static JsonAppender<? super JsonBuilder.Object> analyzerAppender(
			RemoteAnalyzerReference originalAnalyzerReference, RemoteAnalyzerReference queryAnalyzerReference, String fieldName) {
		String originalAnalyzerName = originalAnalyzerReference.getAnalyzerName( fieldName );
		String queryAnalyzerName = queryAnalyzerReference.getAnalyzerName( fieldName );
		if ( !originalAnalyzerName.equals( queryAnalyzerName ) ) {
			if ( queryAnalyzerReference.isNormalizer( fieldName ) ) {
				throw new AssertionFailure(
						"Hibernate Search should not try to explicitly override normalizers in search queries"
						+ "; got normalizer '" + queryAnalyzerName + "' for field '" + fieldName + "'" );
			}
			return builder -> builder.addProperty( "analyzer", queryAnalyzerName );
		}
		else {
			return NOOP_APPENDER;
		}
	}

	/**
	 * Appender that adds a "slop" property if necessary.
	 */
	private static JsonBuilder.JsonAppender<? super JsonBuilder.Object> slopAppender(final int slop) {
		if ( slop != DEFAULT_SLOP ) {
			return builder -> builder.addProperty( "slop", slop );
		}
		else {
			return NOOP_APPENDER;
		}
	}

	/**
	 * Appender that adds a "fuzziness" property if necessary.
	 */
	private static JsonBuilder.JsonAppender<? super JsonBuilder.Object> fuzzinessAppender(final int maxEditDistance) {
		if ( maxEditDistance != DEFAULT_MAX_EDIT_DISTANCE ) {
			return builder -> builder.addProperty( "fuzziness", maxEditDistance );
		}
		else {
			return NOOP_APPENDER;
		}
	}

	/**
	 * Appender that adds a "boost" property if necessary.
	 */
	private static JsonBuilder.JsonAppender<? super JsonBuilder.Object> boostAppender(Query query) {
		final float boost = query.getBoost();
		if ( boost != DEFAULT_BOOST ) { // We actually want to use float equality here
			return builder -> builder.addProperty( "boost", boost );
		}
		else {
			return NOOP_APPENDER;
		}
	}

	private static JsonObject wrapQueryForNestedIfRequired(String field, JsonObject query) {
		if ( !isNested( field ) ) {
			return query;
		}
		String path = FieldHelper.getEmbeddedFieldPath( field );

		return JsonBuilder.object().add( "nested",
				JsonBuilder.object()
						.addProperty( "path", path )
						.add( "query", query ) )
				.build();
	}

	private static boolean isNested(String field) {
		//TODO Drive through meta-data
//		return FieldHelper.isEmbeddedField( field );
		return false;
	}

	/**
	 * Convert a Lucene {@link Sort} to an Elasticsearch sort, trying to preserve
	 * the exact same meaning as the Sort would have in Lucene.
	 *
	 * <p>For instance, missing values on numeric fields are implicitly 0 in Lucene, so this
	 * method will add it explicitly on any numeric Elasticsearch sort.
	 *
	 * @param sort The Lucene {@link Sort} to convert
	 * @return The equivalent Elasticsearch sort, as a {@link JsonArray}
	 */
	public static JsonArray fromLuceneSort(Sort sort) {
		JsonBuilder.Array builder = JsonBuilder.array();
		for ( SortField field : sort.getSort() ) {
			builder.add( fromLuceneSortField( field ) );
		}
		return builder.build();
	}

	private static JsonBuilder.Object fromLuceneSortField(SortField sortField) {
		if ( sortField instanceof DistanceSortField ) {
			DistanceSortField distanceSortField = (DistanceSortField) sortField;
			Coordinates center = distanceSortField.getCenter();
			return JsonBuilder.object().add( "_geo_distance", JsonBuilder.object()
					.add( "order", fromLuceneSortFieldOrder( sortField.getType(), sortField.getReverse() ) )
					.add( sortField.getField(), JsonBuilder.object()
							.addProperty( "lat", center.getLatitude() )
							.addProperty( "lon", center.getLongitude() )
					)
					.addProperty( "unit", "km" )
					.addProperty( "distance_type", "arc" ) );
		}
		else if ( sortField instanceof NativeSortField ) {
			NativeSortField nativeSortField = (NativeSortField) sortField;
			String sortFieldName = nativeSortField.getField();
			String sortDescriptionAsString = nativeSortField.getNativeSortDescription();
			JsonElement sortDescription = JSON_PARSER.parse( sortDescriptionAsString );
			return JsonBuilder.object().add( sortFieldName, sortDescription );
		}
		else {
			SortField.Type sortFieldType = sortField.getType();

			String sortFieldName;
			if ( sortField.getField() == null ) {
				switch ( sortFieldType ) {
					case DOC:
						sortFieldName = "_uid";
						break;
					case SCORE:
						sortFieldName = "_score";
						break;
					default:
						throw LOG.cannotUseThisSortTypeWithNullSortFieldName( sortField.getType() );
				}
			}
			else {
				sortFieldName = sortField.getField();
			}

			boolean reverse = sortField.getReverse();
			JsonElement order = fromLuceneSortFieldOrder( sortField.getType(), reverse );

			JsonBuilder.Object contentBuilder = JsonBuilder.object()
					.add( "order", order );

			JsonElement missing = fromLuceneSortFieldMissing( sortFieldType, sortField.missingValue, reverse );
			if ( missing != null ) {
				contentBuilder.add( "missing", missing );
			}


			return JsonBuilder.object().add( sortFieldName, contentBuilder );
		}
	}

	private static JsonPrimitive fromLuceneSortFieldOrder(Type sortFieldType, boolean reverse) {
		switch ( sortFieldType ) {
			case SCORE:
				return reverse ? SORT_ORDER_ASC : SORT_ORDER_DESC;
			default:
				return reverse ? SORT_ORDER_DESC : SORT_ORDER_ASC;
		}
	}

	private static JsonPrimitive fromLuceneSortFieldMissing(Type sortFieldType, Object luceneMissing, boolean reverse) {
		if ( luceneMissing == null ) {
			/*
			 * Simulate Lucene's behavior of assigning default missing values when none is explicitly provided.
			 */
			switch ( sortFieldType ) {
				case DOUBLE:
				case FLOAT:
				case INT:
				case LONG:
					luceneMissing = SORT_FIELD_SCALAR_DEFAULTS.get( sortFieldType );
					break;
				case STRING:
				case STRING_VAL:
					luceneMissing = reverse ? SortField.STRING_LAST : SortField.STRING_FIRST;
					break;
				default:
					break;
			}
		}

		switch ( sortFieldType ) {
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
				// Use a more natural representation of the missing value, if possible
				if ( luceneMissing.equals( SORT_FIELD_SCALAR_MINIMUMS.get( sortFieldType ) ) ) {
					return reverse ? SORT_MISSING_LAST : SORT_MISSING_FIRST;
				}
				else if ( luceneMissing.equals( SORT_FIELD_SCALAR_MAXIMUMS.get( sortFieldType ) ) ) {
					return reverse ? SORT_MISSING_FIRST : SORT_MISSING_LAST;
				}
				else {
					// Make sure the correct type is used (and throw a ClassCastException if not, as Lucene does)
					return new JsonPrimitive( SORT_FIELD_SCALAR_TYPES.get( sortFieldType ).cast( luceneMissing ) );
				}
			case STRING:
			case STRING_VAL:
				if ( SortField.STRING_LAST.equals( luceneMissing ) ) {
					return SORT_MISSING_LAST;
				}
				else if ( SortField.STRING_FIRST.equals( luceneMissing ) ) {
					return SORT_MISSING_FIRST;
				}
				else if ( luceneMissing != null ) {
					throw new AssertionFailure( "Unexpected missing value specified on a String SortField: " + luceneMissing );
				}
				else {
					return null;
				}
			default:
				if ( luceneMissing != null ) {
					throw new AssertionFailure( "Missing value specified on a SortField which is not supposed to support it: " + sortFieldType );
				}
				else {
					return null;
				}
		}
	}
}
