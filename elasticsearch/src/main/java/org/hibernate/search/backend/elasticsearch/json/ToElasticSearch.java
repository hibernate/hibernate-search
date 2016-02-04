/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.json;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.query.dsl.impl.DiscreteFacetRequest;
import org.hibernate.search.query.dsl.impl.FacetRange;
import org.hibernate.search.query.dsl.impl.RangeFacetRequest;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Various utilities to transform Hibernate Search API into Elastic Search JSON.
 *
 * @author Guillaume Smet
 * @author Gunnar Morling
 */
public class ToElasticSearch {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private ToElasticSearch() {
	}

	public static JsonObject matchAll() {
		return JsonBuilder.object().add( "match_all", new JsonObject() ).build();
	}

	public static void addFacetingRequest(JsonBuilder.Object jsonQuery, FacetingRequest facetingRequest) {
		if ( facetingRequest instanceof DiscreteFacetRequest ) {
			String field = facetingRequest.getFieldName();

			JsonObject termsJsonQuery = JsonBuilder.object().add( "terms",
					JsonBuilder.object()
							.addProperty( "field", field )
							.addProperty( "size", facetingRequest.getMaxNumberOfFacets() == -1 ? 0 : facetingRequest.getMaxNumberOfFacets() )
							.add( "order", fromFacetSortOrder( facetingRequest.getSort() ) )
							.addProperty( "min_doc_count", facetingRequest.hasZeroCountsIncluded() ? 0 : 1 )
					).build();

			if ( isFieldNested( field ) ) {
				JsonBuilder.Object facetJsonQuery = JsonBuilder.object();
				facetJsonQuery.add( "nested", JsonBuilder.object()
								.addProperty( "path", getFieldNestedPath( field ) ) );
				facetJsonQuery.add( "aggregations", JsonBuilder.object().add( facetingRequest.getFacetingName(), termsJsonQuery));
				jsonQuery.add( facetingRequest.getFacetingName(), facetJsonQuery);
			}
			else {
				jsonQuery.add( facetingRequest.getFacetingName(), termsJsonQuery );
			}
		}
		else if ( facetingRequest instanceof RangeFacetRequest<?> ) {
			RangeFacetRequest<?> rangeFacetingRequest = (RangeFacetRequest<?>) facetingRequest;
			for ( FacetRange<?> facetRange : rangeFacetingRequest.getFacetRangeList() ) {
				JsonBuilder.Object rangeQuery = JsonBuilder.object();
				if ( facetRange.getMin() != null ) {
					rangeQuery.addProperty( facetRange.isMinIncluded() ? "gte" : "gt", facetRange.getMin() );
				}
				if ( facetRange.getMax() != null ) {
					rangeQuery.addProperty( facetRange.isMaxIncluded() ? "lte" : "lt", facetRange.getMax() );
				}

				jsonQuery.add( facetingRequest.getFacetingName() + "-" + facetRange.hashCode(),
						JsonBuilder.object().add( "filter",
								JsonBuilder.object().add( "range",
										JsonBuilder.object().add( facetingRequest.getFieldName(),
												rangeQuery)))).build();
			}
		}
		else {
			throw new IllegalArgumentException( "Faceting request of type " + facetingRequest.getClass().getName() + " not supported" );
		}
	}

	// TODO GSM: see if Lucene query translations is sufficient to get everything working.
//	public static JsonObject fromFacetSelection(FacetSelection selection) {
//		JsonBuilder.Array query = JsonBuilder.array();
//		for ( Facet facet : selection.getSelectedFacets() ) {
//			query.add( fromLuceneQuery( facet.getFacetQuery() ) );
//		}
//		if ( query.size() == 0 ) {
//			return null;
//		}
//		return condition( selection.getOccurType().name().toLowerCase(), query.build() );
//	}

//	public static JsonObject queryFromFacet(Facet facet) {
//		if ( facet instanceof RangeFacet ) {
//			RangeFacet<?> rangeFacet = (RangeFacet<?>) facet;
//			JsonBuilder.Object rangeQuery = JsonBuilder.object();
//			if ( rangeFacet.getMin() != null ) {
//				if ( rangeFacet.isIncludeMin() ) {
//					rangeQuery.addProperty( "gte", rangeFacet.getMin() );
//				}
//				else {
//					rangeQuery.addProperty( "gt", rangeFacet.getMin() );
//				}
//			}
//			if ( rangeFacet.getMax() != null ) {
//				if ( rangeFacet.isIncludeMax() ) {
//					rangeQuery.addProperty( "lte", rangeFacet.getMax() );
//				}
//				else {
//					rangeQuery.addProperty( "lt", rangeFacet.getMax() );
//				}
//			}
//			return JsonBuilder.object().add( "range",
//					JsonBuilder.object().add( facet.getFieldName(), rangeQuery ) ).build();
//		}
//		else {
//			return JsonBuilder.object().add( "term",
//					JsonBuilder.object().addProperty( facet.getFieldName(), facet.getValue() ) )
//					.build();
//		}
//	}

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
				// TODO implement
				throw new UnsupportedOperationException( "Not yet implemented" );
		}
		return sort;
	}

	public static JsonObject condition(String operator, JsonArray conditions) {
		JsonObject jsonCondition = new JsonObject();
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

		throw LOG.cannotTransformLuceneQueryIntoEsQuery( query );
	}

	private static JsonObject convertMatchAllDocsQuery(MatchAllDocsQuery matchAllDocsQuery) {
		JsonObject matchAll = new JsonObject();
		matchAll.add( "match_all", new JsonObject() );
		return matchAll;
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

	private static JsonObject convertTermQuery(TermQuery termQuery) {
		String field = termQuery.getTerm().field();

		JsonObject term = new JsonObject();
		term.addProperty( field, termQuery.getTerm().text() );

		JsonObject matchQuery = new JsonObject();
		matchQuery.add( "match", term );

		// prepare query on nested property
		if ( field.contains( "." ) ) {
			String path = field.substring( 0, field.lastIndexOf( "." ) );

			JsonObject nested = new JsonObject();
			nested.addProperty( "path", path );
			nested.add( "query", matchQuery );

			matchQuery = new JsonObject();
			matchQuery.add( "nested", nested );
		}

		return matchQuery;
	}

	private static JsonObject convertWildcardQuery(WildcardQuery query) {
		String field = query.getTerm().field();

		JsonObject term = new JsonObject();
		term.addProperty( field, query.getTerm().text() );

		JsonObject wildcardQuery = new JsonObject();
		wildcardQuery.add( "wildcard", term );

		// prepare query on nested property
		if ( isFieldNested( field ) ) {
			String path = getFieldNestedPath( field );

			JsonObject nested = new JsonObject();
			nested.addProperty( "path", path );
			nested.add( "query", wildcardQuery );

			wildcardQuery = new JsonObject();
			wildcardQuery.add( "nested", nested );
		}

		return wildcardQuery;
	}

	private static JsonObject convertTermRangeQuery(TermRangeQuery query) {
		JsonObject interval = new JsonObject();

		if ( query.getLowerTerm() != null ) {
			interval.addProperty( query.includesLower() ? "gte" : "gt", query.getLowerTerm().utf8ToString() );
		}
		if ( query.getUpperTerm() != null ) {
			interval.addProperty( query.includesUpper() ? "lte" : "lt", query.getUpperTerm().utf8ToString() );
		}

		JsonObject term = new JsonObject();
		term.add( query.getField(), interval );

		JsonObject range = new JsonObject();
		range.add( "range", term );

		return range;
	}

	private static JsonObject convertNumericRangeQuery(NumericRangeQuery<?> query) {
		JsonObject interval = new JsonObject();
		interval.addProperty( query.includesMin() ? "gte" : "gt", query.getMin() );
		interval.addProperty( query.includesMax() ? "lte" : "lt", query.getMax() );

		JsonObject term = new JsonObject();
		term.add( query.getField(), interval );

		JsonObject range = new JsonObject();
		range.add( "range", term );

		return range;
	}

	public static JsonObject fromLuceneFilter(Filter luceneFilter) {
		if ( luceneFilter instanceof QueryWrapperFilter ) {
			return fromLuceneQuery( ( (QueryWrapperFilter) luceneFilter ).getQuery() );
		}
		throw LOG.cannotTransformLuceneFilterIntoEsQuery( luceneFilter );
	}

	private static boolean isFieldNested(String field) {
		return field.contains( "." );
	}

	private static String getFieldNestedPath(String field) {
		return field.substring( 0, field.lastIndexOf( "." ) );
	}

}
