/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.hibernate.search.backend.elasticsearch.client.impl.JestClientHolder;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.filter.FullTextFilter;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.query.engine.impl.AbstractHSQuery;
import org.hibernate.search.query.engine.impl.EntityInfoImpl;
import org.hibernate.search.query.engine.impl.TimeoutManagerImpl;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * Query implementation based on ElasticSearch.
 *
 * @author Gunnar Morling
 */
public class ElasticSearchHSQueryImpl extends AbstractHSQuery {

	private static final Log LOG = LoggerFactory.make();

	private final String jsonQuery;

	public ElasticSearchHSQueryImpl(String jsonQuery, ExtendedSearchIntegrator extendedIntegrator) {
		super( extendedIntegrator );
		this.jsonQuery = jsonQuery;
	}

	@Override
	public HSQuery luceneQuery(Query query) {
		throw new UnsupportedOperationException( "Cannot use Lucene query with ElasticSearch" );
	}

	@Override
	public FacetManager getFacetManager() {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public Query getLuceneQuery() {
		throw new UnsupportedOperationException( "Cannot use Lucene query with ElasticSearch" );
	}

	@Override
	public DocumentExtractor queryDocumentExtractor() {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public int queryResultSize() {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public Explanation explain(int documentId) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public HSQuery setSpatialParameters(Coordinates center, String fieldName) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public HSQuery tenantIdentifier(String tenantId) {
		if ( tenantId != null ) {
			LOG.warnf( "Multi-tenancy not yet implemented for ElasticSearch backend" );
		}

		return this;
	}

	@Override
	public HSQuery sort(Sort sort) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public HSQuery filter(Filter filter) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public HSQuery projection(String... fields) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public HSQuery firstResult(int firstResult) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public HSQuery maxResults(int maxResults) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public FullTextFilter enableFullTextFilter(String name) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public void disableFullTextFilter(String name) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	protected void clearCachedResults() {
		// Nothing to do
	}

	@Override
	protected TimeoutManagerImpl buildTimeoutManager() {
		return new TimeoutManagerImpl(
				jsonQuery,
				timeoutExceptionFactory,
				this.extendedIntegrator.getTimingSource()
		);
	}

	@Override
	public List<EntityInfo> queryEntityInfos() {
		Search.Builder search = new Search.Builder( jsonQuery );
		Map<String, Class<?>> entityTypesByName = new HashMap<>();

		if ( indexedTargetedEntities == null || indexedTargetedEntities.isEmpty() ) {
			for ( Entry<Class<?>, EntityIndexBinding> binding : extendedIntegrator.getIndexBindings().entrySet() ) {
				entityTypesByName.put( binding.getKey().getName(), binding.getKey() );

				IndexManager[] indexManagers = binding.getValue().getIndexManagers();
				for (IndexManager indexManager : indexManagers) {
					ElasticSearchIndexManager esIndexManager = (ElasticSearchIndexManager) indexManager;
					search.addIndex( esIndexManager.getActualIndexName() );
				}
			}
		}
		else {
			for ( Class<?> entityType : indexedTargetedEntities ) {
				entityTypesByName.put( entityType.getName(), entityType );

				EntityIndexBinding binding = extendedIntegrator.getIndexBinding( entityType );
				IndexManager[] indexManagers = binding.getIndexManagers();

				for (IndexManager indexManager : indexManagers) {
					ElasticSearchIndexManager esIndexManager = (ElasticSearchIndexManager) indexManager;
					search.addIndex( esIndexManager.getActualIndexName() );
				}
			}
		}

		SearchResult searchResult = executeRequest( search.build() );
		List<EntityInfo> results = new ArrayList<>( searchResult.getTotal() );
		JsonArray hits = searchResult.getJsonObject().get( "hits" ).getAsJsonObject().get( "hits" ).getAsJsonArray();

		for (JsonElement hit : hits) {
			String type = hit.getAsJsonObject().get( "_type" ).getAsString();
			Class<?> clazz = entityTypesByName.get( type );
			EntityIndexBinding binding = extendedIntegrator.getIndexBinding( clazz );
			Object id = getId( hit, binding );

			results.add( new EntityInfoImpl( clazz, binding.getDocumentBuilder().getIdentifierName(), (Serializable) id, null ) );
		}

		return results;
	}

	private Object getId(JsonElement hit, EntityIndexBinding binding) {
		Document tmp = new Document();
		tmp.add( new StringField( "id", hit.getAsJsonObject().get( "_id" ).getAsString(), Store.NO) );
		Object id = binding.getDocumentBuilder().getIdBridge().get( "id", tmp );

		return id;
	}

	private <T extends JestResult > T executeRequest(Action<T> request) {
		T result;
		try {
			result = JestClientHolder.getClient().execute( request );

			System.out.println( result.getJsonString() );

			if ( !result.isSucceeded() ) {
				throw new SearchException( result.getErrorMessage() );
			}

			return result;
		}
		catch (IOException e) {
			throw new SearchException( e );
		}
	}
}
