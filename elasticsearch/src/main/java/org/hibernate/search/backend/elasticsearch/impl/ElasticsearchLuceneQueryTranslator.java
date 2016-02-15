/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.Properties;
import java.util.Set;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.hibernate.search.backend.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.query.engine.impl.LuceneQueryTranslator;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Translates Lucene queries into ES queries.
 * <p>
 * Extra-experimental ;)
 *
 * @author Gunnar Morling
 */
public class ElasticsearchLuceneQueryTranslator implements LuceneQueryTranslator, Startable {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private ExtendedSearchIntegrator extendedIntegrator;

	@Override
	public void start(Properties properties, BuildContext context) {
		extendedIntegrator = context.getUninitializedSearchIntegrator();
	}

	@Override
	public QueryDescriptor convertLuceneQuery(Query luceneQuery) {
		JsonObject convertedQuery = convertQuery( luceneQuery );

		JsonObject query = new JsonObject();
		query.add( "query", convertedQuery );

		return ElasticsearchQueries.fromJson( query.toString() );
	}

	private JsonObject convertQuery(Query query) {
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

	@Override
	public boolean conversionRequired(Class<?>... entities) {
		Set<Class<?>> queriedEntityTypes = getQueriedEntityTypes( entities );
		Set<Class<?>> queriedEntityTypesWithSubTypes = extendedIntegrator.getIndexedTypesPolymorphic( queriedEntityTypes.toArray( new Class<?>[queriedEntityTypes.size()] ) );

		for ( Class<?> queriedEntityType : queriedEntityTypesWithSubTypes ) {
			EntityIndexBinding binding = extendedIntegrator.getIndexBinding( queriedEntityType );

			if ( binding == null ) {
				continue;
			}

			IndexManager[] indexManagers = binding.getIndexManagers();

			for ( IndexManager indexManager : indexManagers ) {
				if ( indexManager instanceof ElasticsearchIndexManager ) {
					return true;
				}
			}
		}

		return false;
	}

	private JsonObject convertMatchAllDocsQuery(MatchAllDocsQuery matchAllDocsQuery) {
		JsonObject matchAll = new JsonObject();
		matchAll.add( "match_all", new JsonObject() );
		return matchAll;
	}

	private JsonObject convertBooleanQuery(BooleanQuery booleanQuery) {
		JsonArray musts = new JsonArray();
		JsonArray shoulds = new JsonArray();
		JsonArray mustNots = new JsonArray();
		JsonArray filters = new JsonArray();

		for ( BooleanClause clause : booleanQuery.clauses() ) {
			switch ( clause.getOccur() ) {
				case MUST:
					musts.add( convertQuery( clause.getQuery() ) );
					break;
				case FILTER:
					filters.add( convertQuery( clause.getQuery() ) );
					break;
				case MUST_NOT:
					mustNots.add( convertQuery( clause.getQuery() ) );
					break;
				case SHOULD:
					shoulds.add( convertQuery( clause.getQuery() ) );
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

	private JsonObject convertTermQuery(TermQuery termQuery) {
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

	private JsonObject convertWildcardQuery(WildcardQuery query) {
		String field = query.getTerm().field();

		JsonObject term = new JsonObject();
		term.addProperty( field, query.getTerm().text() );

		JsonObject wildcardQuery = new JsonObject();
		wildcardQuery.add( "wildcard", term );

		// prepare query on nested property
		if ( field.contains( "." ) ) {
			String path = field.substring( 0, field.lastIndexOf( "." ) );

			JsonObject nested = new JsonObject();
			nested.addProperty( "path", path );
			nested.add( "query", wildcardQuery );

			wildcardQuery = new JsonObject();
			wildcardQuery.add( "nested", nested );
		}

		return wildcardQuery;
	}

	private JsonObject convertTermRangeQuery(TermRangeQuery query) {
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

	private JsonObject convertNumericRangeQuery(NumericRangeQuery<?> query) {
		JsonObject interval = new JsonObject();
		interval.addProperty( query.includesMin() ? "gte" : "gt", query.getMin() );
		interval.addProperty( query.includesMax() ? "lte" : "lt", query.getMax() );

		JsonObject term = new JsonObject();
		term.add( query.getField(), interval );

		JsonObject range = new JsonObject();
		range.add( "range", term );

		return range;
	}

	private Set<Class<?>> getQueriedEntityTypes(Class<?>... indexedTargetedEntities) {
		if ( indexedTargetedEntities == null || indexedTargetedEntities.length == 0 ) {
			return extendedIntegrator.getIndexBindings().keySet();
		}
		else {
			return CollectionHelper.asSet( indexedTargetedEntities );
		}
	}
}
