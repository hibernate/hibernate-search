/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.backend.elasticsearch.ProjectionConstants;
import org.hibernate.search.backend.elasticsearch.client.impl.JestClient;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.filter.FullTextFilter;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;
import org.hibernate.search.query.engine.impl.AbstractHSQuery;
import org.hibernate.search.query.engine.impl.EntityInfoImpl;
import org.hibernate.search.query.engine.impl.TimeoutManagerImpl;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.searchbox.core.DocumentResult;
import io.searchbox.core.Explain;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.sort.Sort;
import io.searchbox.core.search.sort.Sort.Sorting;

/**
 * Query implementation based on Elasticsearch.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchHSQueryImpl extends AbstractHSQuery {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final Pattern DOT = Pattern.compile( "\\." );

	/**
	 * ES default limit for (firstResult + maxResult)
	 */
	private static final int MAX_RESULT_WINDOW_SIZE = 10000;

	private final String jsonQuery;

	private Integer resultSize;
	private IndexSearcher searcher;
	private SearchResult searchResult;

	public ElasticsearchHSQueryImpl(String jsonQuery, ExtendedSearchIntegrator extendedIntegrator) {
		super( extendedIntegrator );
		this.jsonQuery = jsonQuery;
	}

	@Override
	public HSQuery luceneQuery(Query query) {
		throw new UnsupportedOperationException( "Cannot use Lucene query with Elasticsearch" );
	}

	@Override
	public FacetManager getFacetManager() {
		// TODO implement
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public Query getLuceneQuery() {
		throw new UnsupportedOperationException( "Cannot use Lucene query with Elasticsearch" );
	}

	@Override
	public DocumentExtractor queryDocumentExtractor() {
		return new ElasticsearchDocumentExtractor();
	}

	@Override
	public int queryResultSize() {
		if ( searchResult == null ) {
			execute();
		}

		return resultSize;
	}

	@Override
	public Explanation explain(int documentId) {
		if ( searchResult == null ) {
			execute();
		}

		JsonObject hit = searchResult.getJsonObject()
				.get( "hits" )
				.getAsJsonObject()
				.get( "hits" )
				.getAsJsonArray()
				// TODO Is it right to use the document id that way? I am not quite clear about its semantics
				.get( documentId )
				.getAsJsonObject();

		try ( ServiceReference<JestClient> client = getExtendedSearchIntegrator().getServiceManager().requestReference( JestClient.class ) ) {
			Explain request = new Explain.Builder(
					hit.get( "_index" ).getAsString(),
					hit.get( "_type" ).getAsString(),
					hit.get( "_id" ).getAsString(),
					searcher.executedQuery
				)
				.build();

			DocumentResult response = client.get().executeRequest( request );
			JsonObject explanation = response.getJsonObject().get( "explanation" ).getAsJsonObject();

			return convertExplanation( explanation );
		}
	}

	private Explanation convertExplanation(JsonObject explanation) {
		float value = explanation.get( "value" ).getAsFloat();
		String description = explanation.get( "description" ).getAsString();
		JsonElement explanationDetails = explanation.get( "details" );

		List<Explanation> details;

		if ( explanationDetails != null ) {
			details = new ArrayList<>( explanationDetails.getAsJsonArray().size() );

			for ( JsonElement detail : explanationDetails.getAsJsonArray() ) {
				details.add( convertExplanation( detail.getAsJsonObject() ) );
			}
		}
		else {
			details = Collections.emptyList();
		}

		return Explanation.match( value, description, details );
	}

	@Override
	public HSQuery setSpatialParameters(Coordinates center, String fieldName) {
		// TODO implement
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public HSQuery filter(Filter filter) {
		// TODO implement
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public FullTextFilter enableFullTextFilter(String name) {
		// TODO implement
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public void disableFullTextFilter(String name) {
		// TODO implement
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
		if ( searchResult == null ) {
			execute();
		}

		List<EntityInfo> results = new ArrayList<>( searchResult.getTotal() );
		JsonArray hits = searchResult.getJsonObject().get( "hits" ).getAsJsonObject().get( "hits" ).getAsJsonArray();

		for ( JsonElement hit : hits ) {
			EntityInfo entityInfo = searcher.convertQueryHit( hit.getAsJsonObject() );
			if ( entityInfo != null ) {
				results.add( entityInfo );
			}
		}

		return results;
	}

	private void execute() {
		searcher = new IndexSearcher();

		searchResult = searcher.runSearch();
		resultSize = searchResult.getTotal();
	}

	private Object getId(JsonObject hit, EntityIndexBinding binding) {
		Document tmp = new Document();
		tmp.add( new StringField( "id", DocumentIdHelper.getEntityId( hit.get( "_id" ).getAsString() ), Store.NO) );
		Object id = binding.getDocumentBuilder().getIdBridge().get( "id", tmp );

		return id;
	}

	/**
	 * Determines the affected indexes and runs the given query against them.
	 */
	private class IndexSearcher {

		private final Search search;
		private final Map<String, Class<?>> entityTypesByName;
		private final String executedQuery;

		private IndexSearcher() {
			entityTypesByName = new HashMap<>();
			String idFieldName = null;
			JsonArray typeFilters = new JsonArray();
			Set<String> indexNames = new HashSet<>();

			for ( Class<?> queriedEntityType : getQueriedEntityTypes() ) {
				entityTypesByName.put( queriedEntityType.getName(), queriedEntityType );

				EntityIndexBinding binding = extendedIntegrator.getIndexBinding( queriedEntityType );
				IndexManager[] indexManagers = binding.getIndexManagers();

				for ( IndexManager indexManager : indexManagers ) {
					if ( !( indexManager instanceof ElasticsearchIndexManager ) ) {
						throw LOG.cannotRunEsQueryTargetingEntityIndexedWithNonEsIndexManager(
							queriedEntityType,
							jsonQuery
						);
					}

					// TODO will this be a problem when querying multiple entity types, with one using a field as id
					// field and the other not; is that possible?
					idFieldName = binding.getDocumentBuilder().getIdentifierName();
					ElasticsearchIndexManager esIndexManager = (ElasticsearchIndexManager) indexManager;
					indexNames.add( esIndexManager.getActualIndexName() );
				}

				typeFilters.add( getEntityTypeFilter( queriedEntityType ) );
			}

			// Query filters; always a type filter, possibly a tenant id filter;
			// TODO feed in user-provided filters
			JsonObject effectiveFilter = getEffectiveFilter( typeFilters );

			// TODO can we avoid the forth and back between GSON and String?
			executedQuery = "{ \"query\" : { \"filtered\" : { " + jsonQuery.substring( 1, jsonQuery.length() - 1 ) + ", \"filter\" : " + effectiveFilter.toString() + " } } }";

			Search.Builder search = new Search.Builder( executedQuery );
			search.addIndex( indexNames );
			search.setParameter( "from", firstResult );

			// If the user has given a value, take it as is, let ES itself complain if it's too high; if no value is
			// given, I take as much as possible, as by default only 10 rows would be returned
			search.setParameter( "size", maxResults != null ? maxResults : MAX_RESULT_WINDOW_SIZE - firstResult );

			// TODO: embedded fields
			if ( sort != null ) {
				for ( SortField sortField : sort.getSort() ) {
					String sortFieldName = sortField.getField().equals( idFieldName ) ? "_uid" : sortField.getField();
					search.addSort( new Sort( sortFieldName, sortField.getReverse() ? Sorting.DESC : Sorting.ASC ) );
				}
			}
			this.search = search.build();
		}

		private JsonObject getEffectiveFilter(JsonArray typeFilters) {
			JsonArray filters = new JsonArray();

			JsonObject tenantFilter = getTenantIdFilter();
			if ( tenantFilter != null ) {
				filters.add( tenantFilter );
			}

			// wrap type filters into should if there is more than one
			JsonObject effectiveTypeFilter = new JsonObject();
			if ( typeFilters.size() == 1 ) {
				effectiveTypeFilter = typeFilters.get( 0 ).getAsJsonObject();
			}
			else {
				JsonObject should = new JsonObject();
				should.add( "should", typeFilters );
				effectiveTypeFilter = new JsonObject();
				effectiveTypeFilter.add( "bool", should );
			}
			filters.add( effectiveTypeFilter );

			// wrap filters into must if there is more than one
			JsonObject effectiveFilter = new JsonObject();
			if ( filters.size() == 1 ) {
				effectiveFilter = filters.get( 0 ).getAsJsonObject();
			}
			else {
				JsonObject must = new JsonObject();
				must.add( "must", filters );
				effectiveFilter = new JsonObject();
				effectiveFilter.add( "bool", must );
			}

			return effectiveFilter;
		}

		private JsonObject getEntityTypeFilter(Class<?> queriedEntityType) {
			JsonObject value = new JsonObject();
			value.addProperty( "value", queriedEntityType.getName() );

			JsonObject type = new JsonObject();
			type.add( "type", value );

			return type;
		}

		private JsonObject getTenantIdFilter() {
			if ( tenantId == null ) {
				return null;
			}

			JsonObject value = new JsonObject();
			value.addProperty( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, tenantId );

			JsonObject tenantFilter = new JsonObject();
			tenantFilter.add( "term", value );

			return tenantFilter;
		}

		private Iterable<Class<?>> getQueriedEntityTypes() {
			if ( indexedTargetedEntities == null || indexedTargetedEntities.isEmpty() ) {
				return extendedIntegrator.getIndexBindings().keySet();
			}
			else {
				return indexedTargetedEntities;
			}
		}

		SearchResult runSearch() {
			try ( ServiceReference<JestClient> client = getExtendedSearchIntegrator().getServiceManager().requestReference( JestClient.class ) ) {
				return client.get().executeRequest( search );
			}
		}

		EntityInfo convertQueryHit(JsonObject hit) {
			String type = hit.get( "_type" ).getAsString();
			Class<?> clazz = entityTypesByName.get( type );

			if ( clazz == null ) {
				LOG.warnf( "Found unknown type in Elasticsearch index: " + type );
				return null;
			}

			EntityIndexBinding binding = extendedIntegrator.getIndexBinding( clazz );
			Object id = getId( hit, binding );
			Object[] projections = null;
			List<Integer> indexesOfThis = null;

			if ( projectedFields != null ) {
				projections = new Object[projectedFields.length];
				indexesOfThis = new ArrayList<>();

				int i = 0;
				for ( String field : projectedFields ) {
					switch ( field ) {
						case ProjectionConstants.SOURCE:
							projections[i] = hit.getAsJsonObject().get( "_source" ).toString();
							break;
						case ProjectionConstants.DOCUMENT:
							throw new IllegalArgumentException( "Projection of Lucene document not supported with Elasticsearch backend" );
						case DOCUMENT_ID:
							throw new IllegalArgumentException( "Projection of Lucene document id not supported with Elasticsearch backend" );
						case ProjectionConstants.EXPLANATION:
							throw new UnsupportedOperationException( "Not yet implemented" );
						case ProjectionConstants.ID:
							projections[i] = id;
							break;
						case ProjectionConstants.OBJECT_CLASS:
							projections[i] = clazz;
							break;
						case ProjectionConstants.SCORE:
							projections[i] = hit.getAsJsonObject().get( "_score" ).getAsFloat();
							break;
						case ProjectionConstants.SPATIAL_DISTANCE:
							throw new UnsupportedOperationException( "Not yet implemented" );
						case ProjectionConstants.THIS:
							indexesOfThis.add( i );
							break;
						default:
							projections[i] = getFieldValue( binding, hit, field );
					}

					i++;
				}
			}

			EntityInfoImpl entityInfo = new EntityInfoImpl( clazz, binding.getDocumentBuilder().getIdentifierName(), (Serializable) id, projections );

			if ( indexesOfThis != null ) {
				entityInfo.getIndexesOfThis().addAll( indexesOfThis );
			}

			return entityInfo;
		}

		/**
		 * Returns the value of the given field as retrieved from the ES result and converted using the corresponding
		 * field bridge. In case this bridge is not a 2-way bridge, the unconverted value will be returned.
		 */
		private Object getFieldValue(EntityIndexBinding binding, JsonObject hit, String projectedField) {
			DocumentFieldMetadata field = FieldHelper.getFieldMetadata( binding, projectedField );

			if ( field == null ) {
				throw new IllegalArgumentException( "Unknown field " + projectedField + " for entity "
						+ binding.getDocumentBuilder().getMetadata().getType().getName() );
			}

			JsonElement value;

			if ( field.isId() ) {
				value = hit.get( "_id" );
			}
			else {
				value = getFieldValue( hit.get( "_source" ).getAsJsonObject(), projectedField );
			}

			if ( value == null || value.isJsonNull() ) {
				return null;
			}

			return convertFieldValue( binding, field, value );
		}

		private Object convertFieldValue(EntityIndexBinding binding, DocumentFieldMetadata field, JsonElement value) {
			FieldBridge fieldBridge = field.getFieldBridge();

			if ( FieldHelper.isBoolean( binding, field.getName() ) ) {
				return value.getAsBoolean();
			}
			else if ( fieldBridge instanceof TwoWayFieldBridge ) {
				Document tmp = new Document();

				if ( FieldHelper.isNumeric( field ) ) {
					NumericEncodingType numericEncodingType = FieldHelper.getNumericEncodingType( binding, field );

					switch( numericEncodingType ) {
						case INTEGER:
							tmp.add( new IntField( field.getName(), value.getAsInt(), Store.NO ) );
							break;
						case LONG:
							tmp.add( new LongField( field.getName(), value.getAsLong(), Store.NO ) );
							break;
						case FLOAT:
							tmp.add( new FloatField( field.getName(), value.getAsFloat(), Store.NO ) );
							break;
						case DOUBLE:
							tmp.add( new DoubleField( field.getName(), value.getAsDouble(), Store.NO ) );
							break;
						default:
							throw new SearchException( "Unexpected numeric field type: " + binding.getDocumentBuilder().getMetadata().getType() + " "
								+ field.getName() );
					}
				}
				else {
					tmp.add( new StringField( field.getName(), value.getAsString(), Store.NO ) );
				}

				return ( (TwoWayFieldBridge) fieldBridge ).get( field.getName(), tmp );
			}
			// Should only be the case for custom bridges
			else {
				JsonPrimitive primitive = value.getAsJsonPrimitive();

				if ( primitive.isBoolean() ) {
					return primitive.getAsBoolean();
				}
				else if ( primitive.isNumber() ) {
					// TODO this will expose a Gson-specific Number implementation; Can we somehow return an Integer,
					// Long... etc. instead?
					return primitive.getAsNumber();
				}
				else if ( primitive.isString() ) {
					return primitive.getAsString();
				}
				else {
					// TODO Better raise an exception?
					return primitive.toString();
				}
			}
		}

		private JsonElement getFieldValue(JsonObject parent, String projectedField) {
			String field = projectedField;

			if ( projectedField.contains( "." ) ) {
				String[] parts = DOT.split( projectedField );
				field = parts[parts.length - 1];

				for ( int i = 0; i < parts.length - 1; i++ ) {
					JsonElement newParent = parent.get( parts[i] );
					if ( newParent == null ) {
						return null;
					}

					parent = newParent.getAsJsonObject();
				}
			}

			return parent.getAsJsonObject().get( field );
		}
	}

	// TODO: Investigate scrolling API:
	// https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-scroll.html
	private class ElasticsearchDocumentExtractor implements DocumentExtractor {

		private final IndexSearcher searcher;
		private List<EntityInfo> results;

		private ElasticsearchDocumentExtractor() {
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
			throw new UnsupportedOperationException( "TopDocs not available with Elasticsearch backend" );
		}

		private void runSearch() {
			SearchResult searchResult = searcher.runSearch();
			JsonArray hits = searchResult.getJsonObject().get( "hits" ).getAsJsonObject().get( "hits" ).getAsJsonArray();
			results = new ArrayList<>( searchResult.getTotal() );

			for ( JsonElement hit : hits ) {
				EntityInfo converted = searcher.convertQueryHit( hit.getAsJsonObject() );
				if ( converted != null ) {
					results.add( converted );
				}
			}
		}
	}
}
