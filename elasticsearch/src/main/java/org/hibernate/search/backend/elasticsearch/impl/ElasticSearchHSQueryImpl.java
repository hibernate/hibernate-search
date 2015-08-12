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
import io.searchbox.core.search.sort.Sort;
import io.searchbox.core.search.sort.Sort.Sorting;

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
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
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
import com.google.gson.JsonObject;

/**
 * Query implementation based on ElasticSearch.
 *
 * @author Gunnar Morling
 */
public class ElasticSearchHSQueryImpl extends AbstractHSQuery {

	private static final Log LOG = LoggerFactory.make();

	private final String jsonQuery;

	private Integer resultSize;

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
		return new ElasticSearchDocumentExtractor();
	}

	@Override
	public int queryResultSize() {
		if ( resultSize == null ) {
			IndexSearcher searcher = new IndexSearcher();
			SearchResult searchResult = searcher.runSearch();

			resultSize = searchResult.getTotal();
		}

		return resultSize;
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
	public HSQuery filter(Filter filter) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public HSQuery projection(String... fields) {
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
		resultSize = null;
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
		IndexSearcher searcher = new IndexSearcher();
		SearchResult searchResult = searcher.runSearch();

		List<EntityInfo> results = new ArrayList<>( searchResult.getTotal() );
		JsonArray hits = searchResult.getJsonObject().get( "hits" ).getAsJsonObject().get( "hits" ).getAsJsonArray();

		for ( JsonElement hit : hits ) {
			results.add( searcher.convertQueryHit( hit.getAsJsonObject() ) );
		}

		return results;
	}

	private Object getId(JsonObject hit, EntityIndexBinding binding) {
		Document tmp = new Document();
		tmp.add( new StringField( "id", hit.get( "_id" ).getAsString(), Store.NO) );
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

	/**
	 * Determines the affected indexes and runs the given query against them.
	 */
	private class IndexSearcher {

		private final Search search;
		private final Map<String, Class<?>> entityTypesByName;

		private IndexSearcher() {
			Search.Builder search = new Search.Builder( jsonQuery );
			entityTypesByName = new HashMap<>();

			if ( indexedTargetedEntities == null || indexedTargetedEntities.isEmpty() ) {
				for ( Entry<Class<?>, EntityIndexBinding> binding : extendedIntegrator.getIndexBindings().entrySet() ) {
					entityTypesByName.put( binding.getKey().getName(), binding.getKey() );

					IndexManager[] indexManagers = binding.getValue().getIndexManagers();
					for ( IndexManager indexManager : indexManagers ) {
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

					for ( IndexManager indexManager : indexManagers ) {
						ElasticSearchIndexManager esIndexManager = (ElasticSearchIndexManager) indexManager;
						search.addIndex( esIndexManager.getActualIndexName() );
					}
				}
			}

			search.setParameter( "from", firstResult );
			search.setParameter( "size", maxResults != null ? maxResults : Integer.MAX_VALUE );

			// TODO: Id, embedded fields
			if ( sort != null ) {
				for ( SortField sortField : sort.getSort() ) {
					search.addSort( new Sort( sortField.getField(), sortField.getReverse() ? Sorting.DESC : Sorting.ASC ) );
				}
			}
			this.search = search.build();
		}

		SearchResult runSearch() {
			return executeRequest( search );
		}

		EntityInfo convertQueryHit(JsonObject hit) {
			String type = hit.get( "_type" ).getAsString();
			Class<?> clazz = entityTypesByName.get( type );

			if ( clazz == null ) {
				throw new SearchException( "Found unknown type in ElasticSearch index: " + type );
			}

			EntityIndexBinding binding = extendedIntegrator.getIndexBinding( clazz );
			Object id = getId( hit, binding );

			return new EntityInfoImpl( clazz, binding.getDocumentBuilder().getIdentifierName(), (Serializable) id, null );
		}
	}

	// TODO: Investigate scrolling API:
	// https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-scroll.html
	private class ElasticSearchDocumentExtractor implements DocumentExtractor {

		private final IndexSearcher searcher;
		private List<EntityInfo> results;

		private ElasticSearchDocumentExtractor() {
			searcher = new IndexSearcher();
		}

		@Override
		public EntityInfo extract(int index) throws IOException {
			if ( results == null ) {
				runSearch();
			}

			return results.get( index );
		}

		@Override
		public int getFirstIndex() {
			return 0;
		}

		@Override
		public int getMaxIndex() {
			if ( results == null ) {
				runSearch();
			}

			return results.size() - 1;
		}

		@Override
		public void close() {
		}

		@Override
		public TopDocs getTopDocs() {
			throw new UnsupportedOperationException( "TopDocs not available with ElasticSearch backend" );
		}

		private void runSearch() {
			SearchResult searchResult = searcher.runSearch();
			JsonArray hits = searchResult.getJsonObject().get( "hits" ).getAsJsonObject().get( "hits" ).getAsJsonArray();
			results = new ArrayList<>( searchResult.getTotal() );

			for ( JsonElement hit : hits ) {
				results.add( searcher.convertQueryHit( hit.getAsJsonObject() ) );
			}
		}
	}
}
