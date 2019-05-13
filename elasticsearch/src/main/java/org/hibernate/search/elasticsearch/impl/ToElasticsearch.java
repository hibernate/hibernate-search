/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.EnumMap;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.hibernate.search.backend.spi.DeletionQuery;
import org.hibernate.search.elasticsearch.client.impl.ArbitrarySort;
import org.hibernate.search.elasticsearch.client.impl.DistanceSort;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.filter.impl.CachingWrapperFilter;
import org.hibernate.search.query.dsl.impl.DiscreteFacetRequest;
import org.hibernate.search.query.dsl.impl.FacetRange;
import org.hibernate.search.query.dsl.impl.RangeFacetRequest;
import org.hibernate.search.query.dsl.impl.RemoteMatchQuery;
import org.hibernate.search.query.dsl.impl.RemotePhraseQuery;
import org.hibernate.search.query.dsl.sort.impl.NativeSortField;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.spatial.DistanceSortField;
import org.hibernate.search.spatial.impl.DistanceFilter;
import org.hibernate.search.spatial.impl.SpatialHashFilter;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.searchbox.core.search.sort.Sort;
import io.searchbox.core.search.sort.Sort.Missing;
import io.searchbox.core.search.sort.Sort.Sorting;

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

	private static final JsonParser JSON_PARSER = new JsonParser();

	private static final Map<SortField.Type, Object> SORT_FIELD_SCALAR_MINIMUMS = new EnumMap<>( SortField.Type.class );
	private static final Map<SortField.Type, Object> SORT_FIELD_SCALAR_DEFAULTS = new EnumMap<>( SortField.Type.class );
	private static final Map<SortField.Type, Object> SORT_FIELD_SCALAR_MAXIMUMS = new EnumMap<>( SortField.Type.class );
	private static final Map<SortField.Type, Class<?>> SORT_FIELD_SCALAR_TYPES = new EnumMap<>( SortField.Type.class );
	static {
		initSortFieldScalarValues( SortField.Type.DOUBLE, Double.class, Double.MIN_VALUE, 0.0d, Double.MAX_VALUE );
		initSortFieldScalarValues( SortField.Type.FLOAT, Float.class, Float.MIN_VALUE, 0.0f, Float.MAX_VALUE );
		initSortFieldScalarValues( SortField.Type.LONG, Long.class, Long.MIN_VALUE, 0L, Long.MAX_VALUE );
		initSortFieldScalarValues( SortField.Type.INT, Integer.class, Integer.MIN_VALUE, 0, Integer.MAX_VALUE );
	}

	private static void initSortFieldScalarValues(Type type, Class<?> clazz, Object minValue, Object defaultValue, Object maxValue) {
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
							.addProperty( "size", facetingRequest.getMaxNumberOfFacets() < 0 ? Integer.MAX_VALUE : facetingRequest.getMaxNumberOfFacets() )
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
		else if ( query instanceof ConstantScoreQuery ) {
			return convertConstantScoreQuery( (ConstantScoreQuery) query );
		}
		else if ( query instanceof FilteredQuery ) {
			return convertFilteredQuery( (FilteredQuery) query );
		}
		else if ( query instanceof Filter ) {
			return fromLuceneFilter( (Filter) query );
		}
		else if ( query instanceof PhraseQuery ) {
			return convertPhraseQuery( (PhraseQuery) query );
		}

		throw LOG.cannotTransformLuceneQueryIntoEsQuery( query );
	}

	public static JsonObject fromDeletionQuery(DocumentBuilderIndexedEntity documentBuilder, DeletionQuery deletionQuery) {
		return fromLuceneQuery( deletionQuery.toLuceneQuery( documentBuilder ) );
	}

	private static JsonObject convertMatchAllDocsQuery(MatchAllDocsQuery matchAllDocsQuery) {
		return JsonBuilder.object().add( "match_all", new JsonObject() ).build();
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

		JsonObject clauses = new JsonObject();

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

		JsonObject bool = new JsonObject();
		bool.add( "bool", clauses );
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
										.addProperty( "analyzer", query.getAnalyzerReference().getAnalyzer().getName( query.getField() ) )
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
										.addProperty( "analyzer", query.getAnalyzerReference().getAnalyzer().getName( query.getField() ) )
										.append( fuzzinessAppender( query.getMaxEditDistance() ) )
										.append( boostAppender( query ) )
						)
				).build();

		return wrapQueryForNestedIfRequired( query.getField(), matchQuery );
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

	private static JsonObject convertDistanceFilter(DistanceFilter filter) {
		JsonObject distanceQuery = JsonBuilder.object()
				.add( "geo_distance",
						JsonBuilder.object()
								.addProperty( "distance", filter.getRadius() + "km" )
								.add( filter.getCoordinatesField(),
										JsonBuilder.object()
												.addProperty( "lat", filter.getCenter().getLatitude() )
												.addProperty( "lon", filter.getCenter().getLongitude() )
								)
				).build();

		distanceQuery = wrapQueryForNestedIfRequired( filter.getCoordinatesField(), distanceQuery );

		// we only implement the previous filter optimization when we use the hash method as Elasticsearch
		// automatically optimize the geo_distance query with a bounding box filter so we don't need to do it
		// ourselves when we use the range method.
		Filter previousFilter = filter.getPreviousFilter();
		if ( previousFilter instanceof SpatialHashFilter ) {
			distanceQuery = JsonBuilder.object()
					.add( "bool", JsonBuilder.object()
							.add( "must", distanceQuery )
							.add( "filter", convertSpatialHashFilter( (SpatialHashFilter) previousFilter ) )
					).build();
		}

		return distanceQuery;
	}

	private static JsonObject convertSpatialHashFilter(SpatialHashFilter filter) {
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

	private static final JsonBuilder.JsonAppender<Object> NOOP_APPENDER =
			new JsonBuilder.JsonAppender<Object>() {
				@Override
				public void append(Object appendable) {
					// Do nothing
				}
			};

	/**
	 * Appender that adds a "slop" property if necessary.
	 */
	private static JsonBuilder.JsonAppender<? super JsonBuilder.Object> slopAppender(final int slop) {
		if ( slop != DEFAULT_SLOP ) {
			return new JsonBuilder.JsonAppender<JsonBuilder.Object>() {
				@Override
				public void append(JsonBuilder.Object object) {
					object.addProperty( "slop", slop );
				}
			};
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
			return new JsonBuilder.JsonAppender<JsonBuilder.Object>() {
				@Override
				public void append(JsonBuilder.Object object) {
					object.addProperty( "fuzziness", maxEditDistance );
				}
			};
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
			return new JsonBuilder.JsonAppender<JsonBuilder.Object>() {
				@Override
				public void append(JsonBuilder.Object object) {
					object.addProperty( "boost", boost );
				}
			};
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

	public static JsonObject fromLuceneFilter(Filter luceneFilter) {
		if ( luceneFilter instanceof QueryWrapperFilter ) {
			Query query = ( (QueryWrapperFilter) luceneFilter ).getQuery();
			query.setBoost( luceneFilter.getBoost() * query.getBoost() );
			return fromLuceneQuery( query );
		}
		else if ( luceneFilter instanceof DistanceFilter ) {
			return convertDistanceFilter( (DistanceFilter) luceneFilter );
		}
		else if ( luceneFilter instanceof SpatialHashFilter ) {
			return convertSpatialHashFilter( (SpatialHashFilter) luceneFilter );
		}
		else if ( luceneFilter instanceof CachingWrapperFilter ) {
			return fromLuceneFilter( ( (CachingWrapperFilter) luceneFilter ).getCachedFilter() );
		}
		else if ( luceneFilter instanceof org.apache.lucene.search.CachingWrapperFilter ) {
			return fromLuceneFilter( ( (org.apache.lucene.search.CachingWrapperFilter) luceneFilter ).getFilter() );
		}
		throw LOG.cannotTransformLuceneFilterIntoEsQuery( luceneFilter );
	}

	/**
	 * Convert a Lucene {@link SortField} to an Elasticsearch {@link Sort}, trying to preserve
	 * the exact same meaning as the SortField would have in Lucene.
	 *
	 * <p>For instance, missing values on numeric fields are implicitly 0 in Lucene, so this
	 * method will add it explicitly on any numeric Elasticsearch sort.
	 *
	 * @param sortField The Lucene {@link SortField} to convert
	 * @return The equivalent Elasticsearch {@link Sort}
	 */
	public static Sort fromLuceneSortField(SortField sortField) {
		if ( sortField instanceof DistanceSortField ) {
			DistanceSortField distanceSortField = (DistanceSortField) sortField;
			return new DistanceSort( distanceSortField.getField(),
					distanceSortField.getCenter(),
					distanceSortField.getReverse() ? Sorting.DESC : Sorting.ASC );
		}
		else if ( sortField instanceof NativeSortField ) {
			NativeSortField nativeSortField = (NativeSortField) sortField;
			String sortFieldName = nativeSortField.getField();
			String sortDescriptionAsString = nativeSortField.getNativeSortDescription();
			JsonElement sortDescription = JSON_PARSER.parse( sortDescriptionAsString );
			return new ArbitrarySort( sortFieldName, sortDescription );
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
			Sort sort = new Sort( sortFieldName, fromLuceneSortFieldOrder( sortFieldType, reverse ) );
			sort.setMissing( fromLuceneSortFieldMissing( sortFieldType, sortField.missingValue, reverse ) );

			return sort;
		}
	}

	private static Sorting fromLuceneSortFieldOrder(Type sortFieldType, boolean reverse) {
		switch ( sortFieldType ) {
			case SCORE:
				return reverse ? Sorting.ASC : Sorting.DESC;
			default:
				return reverse ? Sorting.DESC : Sorting.ASC;
		}
	}

	private static Object fromLuceneSortFieldMissing(Type sortFieldType, Object luceneMissing, boolean reverse) {
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
					return reverse ? Missing.LAST : Missing.FIRST;
				}
				else if ( luceneMissing.equals( SORT_FIELD_SCALAR_MAXIMUMS.get( sortFieldType ) ) ) {
					return reverse ? Missing.FIRST : Missing.LAST;
				}
				else {
					// Make sure the correct type is used (and throw a ClassCastException if not, as Lucene does)
					return SORT_FIELD_SCALAR_TYPES.get( sortFieldType ).cast( luceneMissing );
				}
			case STRING:
			case STRING_VAL:
				if ( SortField.STRING_LAST.equals( luceneMissing ) ) {
					return Missing.LAST;
				}
				else if ( SortField.STRING_FIRST.equals( luceneMissing ) ) {
					return Missing.FIRST;
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
