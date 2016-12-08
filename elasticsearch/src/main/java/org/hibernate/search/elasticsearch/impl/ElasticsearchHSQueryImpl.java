/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.elasticsearch.ElasticsearchProjectionConstants;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.client.impl.JestClient;
import org.hibernate.search.elasticsearch.filter.ElasticsearchFilter;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper.ExtendedFieldType;
import org.hibernate.search.elasticsearch.util.impl.Window;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.BridgeDefinedField;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.FacetMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.filter.impl.FullTextFilterImpl;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.query.dsl.impl.DiscreteFacetRequest;
import org.hibernate.search.query.dsl.impl.FacetRange;
import org.hibernate.search.query.dsl.impl.RangeFacetRequest;
import org.hibernate.search.query.engine.impl.AbstractHSQuery;
import org.hibernate.search.query.engine.impl.EntityInfoImpl;
import org.hibernate.search.query.engine.impl.FacetComparators;
import org.hibernate.search.query.engine.impl.FacetManagerImpl;
import org.hibernate.search.query.engine.impl.TimeoutManagerImpl;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.spatial.DistanceSortField;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import io.searchbox.client.JestResult;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Explain;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.SearchScroll;
import io.searchbox.params.Parameters;

/**
 * Query implementation based on Elasticsearch.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchHSQueryImpl extends AbstractHSQuery {

	private static final JsonParser JSON_PARSER = new JsonParser();

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final Pattern DOT = Pattern.compile( "\\." );

	private static final String SPATIAL_DISTANCE_FIELD = "_distance";

	/**
	 * ES default limit for (firstResult + maxResult)
	 */
	private static final int MAX_RESULT_WINDOW_SIZE = 10000;

	private static final String QUERY_PROPERTIES_PREFIX = "hibernate.search.";

	private static final Set<String> SUPPORTED_PROJECTION_CONSTANTS = Collections.unmodifiableSet(
			CollectionHelper.asSet(
					ElasticsearchProjectionConstants.ID,
					ElasticsearchProjectionConstants.OBJECT_CLASS,
					ElasticsearchProjectionConstants.SCORE,
					ElasticsearchProjectionConstants.SOURCE,
					ElasticsearchProjectionConstants.SPATIAL_DISTANCE,
					ElasticsearchProjectionConstants.THIS,
					ElasticsearchProjectionConstants.TOOK,
					ElasticsearchProjectionConstants.TIMED_OUT
			)
	);

	/**
	 * The constructor-provided payload for the Search API, holding
	 * the search query in particular.
	 * <p>
	 * This raw payload will serve as a basis for the actual payload to be sent toElasticsearch,
	 * which will also contain automatically generated data related in particular to sorts
	 * and projections.
	 */
	private final JsonObject rawSearchPayload;

	private Integer resultSize;
	private IndexSearcher searcher;
	private SearchResult searchResult;

	private int sortByDistanceIndex = -1;

	private transient FacetManagerImpl facetManager;

	public ElasticsearchHSQueryImpl(JsonObject rawSearchPayload, ExtendedSearchIntegrator extendedIntegrator) {
		super( extendedIntegrator );
		this.rawSearchPayload = rawSearchPayload;
	}

	@Override
	public HSQuery luceneQuery(Query query) {
		throw LOG.hsQueryLuceneQueryUnsupported();
	}

	@Override
	public FacetManagerImpl getFacetManager() {
		if ( facetManager == null ) {
			facetManager = new FacetManagerImpl( this );
		}
		return facetManager;
	}

	@Override
	public Query getLuceneQuery() {
		throw LOG.hsQueryLuceneQueryUnsupported();
	}

	@Override
	public String getQueryString() {
		return rawSearchPayload.toString();
	}

	@Override
	public DocumentExtractor queryDocumentExtractor() {
		return new ElasticsearchScrollAPIDocumentExtractor();
	}

	SearchResult getSearchResult() {
		if ( searchResult == null ) {
			execute();
		}
		return searchResult;
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
			/*
			 * Do not add every property of the original payload: some properties, such as "_source", do not have the same syntax
			 * and are not necessary to the explanation.
			 */
			JsonObject explainPayload = JsonBuilder.object()
					.add( "query", searcher.payload.get( "query" ) )
					.build();

			Explain request = new Explain.Builder(
					hit.get( "_index" ).getAsString(),
					hit.get( "_type" ).getAsString(),
					hit.get( "_id" ).getAsString(),
					explainPayload
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
	protected void clearCachedResults() {
		searcher = null;
		searchResult = null;
		resultSize = null;
	}

	@Override
	protected TimeoutManagerImpl buildTimeoutManager() {
		return new TimeoutManagerImpl(
				rawSearchPayload,
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
		JsonObject searchResultJsonObject = searchResult.getJsonObject();
		JsonArray hits = searchResultJsonObject.get( "hits" ).getAsJsonObject().get( "hits" ).getAsJsonArray();

		for ( JsonElement hit : hits ) {
			EntityInfo entityInfo = searcher.convertQueryHit( searchResultJsonObject, hit.getAsJsonObject() );
			if ( entityInfo != null ) {
				results.add( entityInfo );
			}
		}

		return results;
	}

	@Override
	protected Set<String> getSupportedProjectionConstants() {
		return SUPPORTED_PROJECTION_CONSTANTS;
	}

	private void execute() {
		searcher = new IndexSearcher();

		searchResult = searcher.search();
		resultSize = searchResult.getTotal();
	}

	/**
	 * Stores all information required to execute the query: query string, relevant indices, query parameters, ...
	 */
	private class IndexSearcher {

		private final Map<String, Class<?>> entityTypesByName = new HashMap<>();
		private final Map<Class<?>, FieldProjection> idProjectionByEntityType = new HashMap<>();
		private final Map<Class<?>, FieldProjection[]> fieldProjectionsByEntityType = new HashMap<>();
		private final Set<String> indexNames = new HashSet<>();
		private final JsonObject payload;
		private final String payloadAsString;

		private IndexSearcher() {
			JsonArray typeFilters = new JsonArray();
			Iterable<Class<?>> queriedEntityTypes = getQueriedEntityTypes();

			for ( Class<?> queriedEntityType : queriedEntityTypes ) {
				entityTypesByName.put( queriedEntityType.getName(), queriedEntityType );

				EntityIndexBinding binding = extendedIntegrator.getIndexBinding( queriedEntityType );
				IndexManager[] indexManagers = binding.getIndexManagers();

				for ( IndexManager indexManager : indexManagers ) {
					if ( !( indexManager instanceof ElasticsearchIndexManager ) ) {
						throw LOG.cannotRunEsQueryTargetingEntityIndexedWithNonEsIndexManager(
							queriedEntityType,
							rawSearchPayload.toString()
						);
					}

					ElasticsearchIndexManager esIndexManager = (ElasticsearchIndexManager) indexManager;
					indexNames.add( esIndexManager.getActualIndexName() );
				}

				typeFilters.add( getEntityTypeFilter( queriedEntityType ) );
			}

			// Query filters; always a type filter, possibly a tenant id filter;
			JsonObject filteredQuery = getFilteredQuery( rawSearchPayload.get( "query" ), typeFilters );

			JsonBuilder.Object payloadBuilder = JsonBuilder.object();
			payloadBuilder.add( "query", filteredQuery );

			addProjections( payloadBuilder );

			if ( !getFacetManager().getFacetRequests().isEmpty() ) {
				JsonBuilder.Object facets = JsonBuilder.object();

				for ( Entry<String, FacetingRequest> facetRequestEntry : getFacetManager().getFacetRequests().entrySet() ) {
					addFacetingRequest( facets, facetRequestEntry.getValue() );
				}

				payloadBuilder.add( "aggregations", facets );
			}

			// Initialize the sortByDistanceIndex to detect if the results are sorted by distance and the position
			// of the sort
			sortByDistanceIndex = getSortByDistanceIndex();
			addScriptFields( payloadBuilder );

			this.payload = payloadBuilder.build();
			payloadAsString = payloadBuilder.build().toString();
		}

		private JsonObject getFilteredQuery(JsonElement originalQuery, JsonArray typeFilters) {
			JsonArray filters = new JsonArray();

			JsonObject tenantFilter = getTenantIdFilter();
			if ( tenantFilter != null ) {
				filters.add( tenantFilter );
			}

			// wrap type filters into should if there is more than one
			filters.add( ToElasticsearch.condition( "should", typeFilters ) );

			// facet filters
			for ( Query query : getFacetManager().getFacetFilters().getFilterQueries() ) {
				filters.add( ToElasticsearch.fromLuceneQuery( query ) );
			}

			// user filter
			if ( userFilter != null ) {
				filters.add( ToElasticsearch.fromLuceneFilter( userFilter ) );
			}

			if ( !filterDefinitions.isEmpty() ) {
				for ( FullTextFilterImpl fullTextFilter : filterDefinitions.values() ) {
					JsonObject filter = buildFullTextFilter( fullTextFilter );
					if ( filter != null ) {
						filters.add( filter );
					}
				}
			}

			JsonBuilder.Object boolBuilder = JsonBuilder.object();

			if ( originalQuery != null && !originalQuery.isJsonNull() ) {
				boolBuilder.add( "must", originalQuery );
			}

			if ( filters.size() == 1 ) {
				boolBuilder.add( "filter", filters.get( 0 ) );
			}
			else {
				boolBuilder.add( "filter", filters );
			}

			return JsonBuilder.object().add( "bool", boolBuilder.build() ).build();
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

		private void addProjections(JsonBuilder.Object payloadBuilder) {
			boolean includeAllSource = false;
			JsonBuilder.Array builder = JsonBuilder.array();

			Iterable<Class<?>> queriedEntityTypes = getQueriedEntityTypes();

			/*
			 * IDs are always projected: always initialize their projections regardless of the
			 * "projectedFields" attribute.
			 */
			for ( Class<?> entityType : queriedEntityTypes ) {
				EntityIndexBinding binding = extendedIntegrator.getIndexBinding( entityType );
				DocumentBuilderIndexedEntity documentBuilder = binding.getDocumentBuilder();
				String idFieldName = documentBuilder.getIdFieldName();
				TypeMetadata typeMetadata = documentBuilder.getTypeMetadata();
				FieldProjection projection = createProjection( builder, typeMetadata, idFieldName );
				idProjectionByEntityType.put( entityType, projection );
			}

			if ( projectedFields != null ) {
				for ( int i = 0 ; i < projectedFields.length ; ++i ) {
					String projectedField = projectedFields[i];
					if ( projectedField == null ) {
						continue;
					}
					switch ( projectedField ) {
						case ElasticsearchProjectionConstants.SOURCE:
							includeAllSource = true;
							break;
						case ElasticsearchProjectionConstants.SCORE:
							// Make sure to compute scores even if we don't sort by relevance
							payloadBuilder.addProperty( "track_scores", true );
							break;
						case ElasticsearchProjectionConstants.ID:
						case ElasticsearchProjectionConstants.THIS:
						case ElasticsearchProjectionConstants.OBJECT_CLASS:
						case ElasticsearchProjectionConstants.SPATIAL_DISTANCE:
						case ElasticsearchProjectionConstants.TOOK:
						case ElasticsearchProjectionConstants.TIMED_OUT:
							// Ignore: no impact on source filtering
							break;
						default:
							for ( Class<?> entityType : queriedEntityTypes ) {
								EntityIndexBinding binding = extendedIntegrator.getIndexBinding( entityType );
								TypeMetadata typeMetadata = binding.getDocumentBuilder().getTypeMetadata();
								FieldProjection projection = createProjection( builder, typeMetadata, projectedField );
								FieldProjection[] projectionsForType = fieldProjectionsByEntityType.get( entityType );
								if ( projectionsForType == null ) {
									projectionsForType = new FieldProjection[projectedFields.length];
									fieldProjectionsByEntityType.put( entityType, projectionsForType );
								}
								projectionsForType[i] = projection;
							}
							break;
					}
				}
			}

			JsonElement filter;
			if ( includeAllSource ) {
				filter = new JsonPrimitive( "*" );
			}
			else {
				JsonArray array = builder.build();
				if ( array.size() > 0 ) {
					filter = array;
				}
				else {
					// Projecting only on score or other document-independent values
					filter = new JsonPrimitive( false );
				}
			}

			payloadBuilder.add( "_source", filter );
		}

		private FieldProjection createProjection(JsonBuilder.Array sourceFilterCollector, TypeMetadata rootTypeMetadata, String projectedField) {
			DocumentFieldMetadata fieldMetadata = rootTypeMetadata.getDocumentFieldMetadataFor( projectedField );
			if ( fieldMetadata != null ) {
				return createProjection( sourceFilterCollector, rootTypeMetadata, fieldMetadata );
			}
			else {
				// We check if it is a field created by a field bridge
				BridgeDefinedField bridgeDefinedField = rootTypeMetadata.getBridgeDefinedFieldMetadataFor( projectedField );
				if ( bridgeDefinedField != null ) {
					return createProjection( sourceFilterCollector, rootTypeMetadata, bridgeDefinedField );
				}
				else {
					/*
					 * No metadata: fall back to dynamically converting the resulting
					 * JSON to the most appropriate Java type.
					 */
					sourceFilterCollector.add( new JsonPrimitive( projectedField ) );
					return new JsonDrivenProjection( projectedField );
				}
			}
		}

		private FieldProjection createProjection(JsonBuilder.Array sourceFilterCollector, TypeMetadata rootTypeMetadata,
				DocumentFieldMetadata fieldMetadata) {
			String absoluteName = fieldMetadata.getAbsoluteName();
			FieldBridge fieldBridge = fieldMetadata.getFieldBridge();
			ExtendedFieldType type = FieldHelper.getType( fieldMetadata );

			if ( ExtendedFieldType.BOOLEAN.equals( type ) ) {
				sourceFilterCollector.add( new JsonPrimitive( absoluteName ) );

				return new PrimitiveProjection( rootTypeMetadata, absoluteName, type );
			}
			else if ( fieldBridge instanceof TwoWayFieldBridge ) {
				sourceFilterCollector.add( new JsonPrimitive( absoluteName ) );

				PrimitiveProjection defaultFieldProjection = new PrimitiveProjection( rootTypeMetadata, absoluteName, type );

				Collection<BridgeDefinedField> bridgeDefinedFields = fieldMetadata.getBridgeDefinedFields().values();
				List<PrimitiveProjection> bridgeDefinedFieldsProjections = CollectionHelper.newArrayList( bridgeDefinedFields.size() );
				for ( BridgeDefinedField bridgeDefinedField : bridgeDefinedFields ) {
					PrimitiveProjection primitiveProjection = createProjection( sourceFilterCollector, rootTypeMetadata, bridgeDefinedField );
					bridgeDefinedFieldsProjections.add( primitiveProjection );
				}
				return new TwoWayFieldBridgeProjection(
						absoluteName, (TwoWayFieldBridge) fieldBridge, defaultFieldProjection, bridgeDefinedFieldsProjections
						);
			}
			else {
				/*
				 * Don't fail immediately: this entity type may not be present in the results, in which case
				 * we don't need to be able to project on this field for this exact entity type.
				 * Just make sure we *will* ultimately fail if we encounter this entity type.
				 */
				return new FailingOneWayFieldBridgeProjection( absoluteName, fieldBridge.getClass() );
			}
		}

		private PrimitiveProjection createProjection(JsonBuilder.Array sourceFilterCollector, TypeMetadata rootTypeMetadata,
				BridgeDefinedField bridgeDefinedField) {
			String absoluteName = bridgeDefinedField.getAbsoluteName();
			ExtendedFieldType type = FieldHelper.getType( bridgeDefinedField );

			sourceFilterCollector.add( new JsonPrimitive( absoluteName ) );

			return new PrimitiveProjection( rootTypeMetadata, absoluteName, type );
		}

		private void addFacetingRequest(JsonBuilder.Object facets, FacetingRequest facetingRequest) {
			String facetFieldAbsoluteName = facetingRequest.getFieldName();
			FacetMetadata facetMetadata = null;
			for ( Class<?> entityType : getQueriedEntityTypes() ) {
				EntityIndexBinding binding = extendedIntegrator.getIndexBinding( entityType );
				TypeMetadata typeMetadata = binding.getDocumentBuilder().getTypeMetadata();
				facetMetadata = typeMetadata.getFacetMetadataFor( facetFieldAbsoluteName );
				if ( facetMetadata != null ) {
					break;
				}
			}

			if ( facetMetadata == null ) {
				throw LOG.unknownFieldNameForFaceting( facetingRequest.getFacetingName(), facetingRequest.getFieldName() );
			}

			String sourceFieldAbsoluteName = facetMetadata.getSourceField().getAbsoluteName();
			String facetSubfieldName = facetMetadata.getPath().getRelativeName();

			ToElasticsearch.addFacetingRequest( facets, facetingRequest, sourceFieldAbsoluteName, facetSubfieldName );
		}

		/**
		 * Returns the index of the DistanceSortField in the Sort array.
		 *
		 * @return the index, -1 if no DistanceSortField has been found
		 */
		private int getSortByDistanceIndex() {
			int i = 0;
			if ( sort != null ) {
				for ( SortField sortField : sort.getSort() ) {
					if ( sortField instanceof DistanceSortField ) {
						return i;
					}
					i++;
				}
			}
			return -1;
		}

		/**
		 * Indicates if the results are sorted by distance (note that it might be a secondary order).
		 */
		private boolean isSortedByDistance() {
			return sortByDistanceIndex >= 0;
		}

		private void addScriptFields(JsonBuilder.Object payloadBuilder) {
			if ( isPartOfProjectedFields( ElasticsearchProjectionConstants.SPATIAL_DISTANCE ) && !isSortedByDistance() ) {
				// when the results are sorted by distance, Elasticsearch returns the distance in a "sort" field in
				// the results. If we don't sort by distance, we need to request for the distance using a script_field.
				payloadBuilder.add( "script_fields",
						JsonBuilder.object().add( SPATIAL_DISTANCE_FIELD, JsonBuilder.object()
							.add( "script", JsonBuilder.object()
								.add( "params",
										JsonBuilder.object()
												.addProperty( "lat", spatialSearchCenter.getLatitude() )
												.addProperty( "lon", spatialSearchCenter.getLongitude() )
								)
								.addProperty( "inline", "doc['" + spatialFieldName + "'] ? doc['" + spatialFieldName + "'].arcDistanceInKm(lat,lon) : null" )
								.addProperty( "lang", "groovy" )
							)
						)
				);
				// in this case, the _source field is not present in the Elasticsearch results
				// we need to ask for it explicitely
				payloadBuilder.add( "fields", JsonBuilder.array().add( new JsonPrimitive( "_source" ) ) );
			}
		}

		SearchResult search() {
			return search( false );
		}

		SearchResult searchWithScrollEnabled() {
			return search( true );
		}

		private SearchResult search(boolean enableScrolling) {
			Search.Builder builder = new Search.Builder( payloadAsString );
			builder.addIndex( indexNames );

			if ( enableScrolling ) {
				/*
				 * Note: "firstResult" is currently being ignored by Elasticsearch when scrolling.
				 * See https://github.com/elastic/elasticsearch/issues/9373
				 * To work this around, we don't use the "from" parameter here, and the document
				 * extractor will skip the results by scrolling until it gets the right index.
				 */

				builder.setParameter( Parameters.SCROLL, getScrollTimeout() );

				/*
				 * The "size" parameter has a slightly different meaning when scrolling: it defines the window
				 * size for the first search *and* for the next calls to the scroll API.
				 * We still reduce the number of results in accordance to "maxResults", but that's done on our side
				 * in the document extractor.
				 */
				builder.setParameter( Parameters.SIZE, getScrollFetchSize() );
			}
			else {
				builder.setParameter( "from", firstResult );

				// If the user has given a value, take it as is, let ES itself complain if it's too high; if no value is
				// given, I take as much as possible, as by default only 10 rows would be returned
				builder.setParameter( Parameters.SIZE, maxResults != null ? maxResults : MAX_RESULT_WINDOW_SIZE - firstResult );
			}

			// TODO: HSEARCH-2254 embedded fields (see https://github.com/searchbox-io/Jest/issues/304)
			if ( sort != null ) {
				validateSortFields( extendedIntegrator, getQueriedEntityTypes() );
				for ( SortField sortField : sort.getSort() ) {
					builder.addSort( ToElasticsearch.fromLuceneSortField( sortField ) );
				}
			}
			Search search = builder.build();
			try ( ServiceReference<JestClient> client = getExtendedSearchIntegrator().getServiceManager().requestReference( JestClient.class ) ) {
				return client.get().executeRequest( search );
			}
		}

		private String getScrollTimeout() {
			return ConfigurationParseHelper.getIntValue(
					getExtendedSearchIntegrator().getConfigurationProperties(),
					QUERY_PROPERTIES_PREFIX + ElasticsearchEnvironment.SCROLL_TIMEOUT,
					ElasticsearchEnvironment.Defaults.SCROLL_TIMEOUT
					) + "s";
		}

		private int getScrollFetchSize() {
			return ConfigurationParseHelper.getIntValue(
					getExtendedSearchIntegrator().getConfigurationProperties(),
					QUERY_PROPERTIES_PREFIX + ElasticsearchEnvironment.SCROLL_FETCH_SIZE,
					ElasticsearchEnvironment.Defaults.SCROLL_FETCH_SIZE
					);
		}

		private int getScrollBacktrackingWindowSize() {
			return ConfigurationParseHelper.getIntValue(
					getExtendedSearchIntegrator().getConfigurationProperties(),
					QUERY_PROPERTIES_PREFIX + ElasticsearchEnvironment.SCROLL_BACKTRACKING_WINDOW_SIZE,
					ElasticsearchEnvironment.Defaults.SCROLL_BACKTRACKING_WINDOW_SIZE
					);
		}

		/**
		 * Scroll through search results, using a previously obtained scrollId.
		 */
		JestResult scroll(String scrollId) {
			SearchScroll.Builder builder = new SearchScroll.Builder( scrollId, getScrollTimeout() );

			SearchScroll scroll = builder.build();
			try ( ServiceReference<JestClient> client = getExtendedSearchIntegrator().getServiceManager().requestReference( JestClient.class ) ) {
				return client.get().executeRequest( scroll );
			}
		}

		JestResult clearScroll(String scrollId) {
			SearchScroll.Builder builder = new SearchScroll.Builder( scrollId, getScrollTimeout() );

			SearchScroll scroll = new SearchScroll( builder ) {
				@Override
				public String getRestMethodName() {
					return "DELETE";
				}
			};

			try ( ServiceReference<JestClient> client = getExtendedSearchIntegrator().getServiceManager().requestReference( JestClient.class ) ) {
				return client.get().executeRequest( scroll );
			}
		}

		EntityInfo convertQueryHit(JsonObject searchResult, JsonObject hit) {
			String type = hit.get( "_type" ).getAsString();
			Class<?> clazz = entityTypesByName.get( type );

			if ( clazz == null ) {
				LOG.warnf( "Found unknown type in Elasticsearch index: " + type );
				return null;
			}

			EntityIndexBinding binding = extendedIntegrator.getIndexBinding( clazz );
			ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
			conversionContext.setClass( clazz );
			FieldProjection idProjection = idProjectionByEntityType.get( clazz );
			Object id = idProjection.convertHit( hit, conversionContext );
			Object[] projections = null;

			if ( projectedFields != null ) {
				projections = new Object[projectedFields.length];

				for ( int i = 0; i < projections.length; i++ ) {
					String field = projectedFields[i];
					if ( field == null ) {
						continue;
					}
					switch ( field ) {
						case ElasticsearchProjectionConstants.SOURCE:
							projections[i] = hit.getAsJsonObject().get( "_source" ).toString();
							break;
						case ElasticsearchProjectionConstants.ID:
							projections[i] = id;
							break;
						case ElasticsearchProjectionConstants.OBJECT_CLASS:
							projections[i] = clazz;
							break;
						case ElasticsearchProjectionConstants.SCORE:
							projections[i] = hit.getAsJsonObject().get( "_score" ).getAsFloat();
							break;
						case ElasticsearchProjectionConstants.SPATIAL_DISTANCE:
							JsonElement distance = null;
							// if we sort by distance, we need to find the index of the DistanceSortField and use it
							// to extract the values from the sort array
							// if we don't sort by distance, we use the field generated by the script_field added earlier
							if ( isSortedByDistance() ) {
								distance = hit.getAsJsonObject().get( "sort" ).getAsJsonArray().get( sortByDistanceIndex );
							}
							else {
								JsonElement fields = hit.getAsJsonObject().get( "fields" );
								if ( fields != null ) { // "fields" seems to be missing if there are only null results in script fields
									distance = hit.getAsJsonObject().get( "fields" ).getAsJsonObject().get( SPATIAL_DISTANCE_FIELD );
								}
							}
							if ( distance == null || distance.isJsonNull() ) {
								projections[i] = null;
							}
							else if ( distance.getAsDouble() == Double.MAX_VALUE ) {
								// When we extract the distance from the sort, its default value is Double.MAX_VALUE
								projections[i] = null;
							}
							else {
								projections[i] = distance.getAsDouble();
							}
							break;
						case ElasticsearchProjectionConstants.TOOK:
							projections[i] = searchResult.get( "took" ).getAsInt();
							break;
						case ElasticsearchProjectionConstants.TIMED_OUT:
							projections[i] = searchResult.get( "timed_out" ).getAsBoolean();
							break;
						case ElasticsearchProjectionConstants.THIS:
							// Use EntityInfo.ENTITY_PLACEHOLDER as placeholder.
							// It will be replaced when we populate
							// the EntityInfo with the real entity.
							projections[i] = EntityInfo.ENTITY_PLACEHOLDER;
							break;
						default:
							FieldProjection projection = fieldProjectionsByEntityType.get( clazz )[i];
							projections[i] = projection.convertHit( hit, conversionContext );
					}
				}
			}

			return new EntityInfoImpl( clazz, binding.getDocumentBuilder().getIdPropertyName(), (Serializable) id, projections );
		}

	}

	private abstract static class FieldProjection {

		/**
		 * Returns the value of the projected field as retrieved from the ES result and converted using the corresponding
		 * field bridge. In case this bridge is not a 2-way bridge, the unconverted value will be returned.
		 */
		public abstract Object convertHit(JsonObject hit, ConversionContext conversionContext);

		protected final JsonElement extractFieldValue(JsonObject parent, String projectedField) {
			String field = projectedField;

			if ( FieldHelper.isEmbeddedField( projectedField ) ) {
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

	private static class TwoWayFieldBridgeProjection extends FieldProjection {

		private final String absoluteName;
		private final TwoWayFieldBridge bridge;
		private final PrimitiveProjection defaultFieldProjection;
		private final List<PrimitiveProjection> bridgeDefinedFieldsProjections;

		public TwoWayFieldBridgeProjection(String absoluteName,
				TwoWayFieldBridge bridge,
				PrimitiveProjection defaultFieldProjection,
				List<PrimitiveProjection> bridgeDefinedFieldsProjections) {
			super();
			this.absoluteName = absoluteName;
			this.bridge = bridge;
			this.defaultFieldProjection = defaultFieldProjection;
			this.bridgeDefinedFieldsProjections = bridgeDefinedFieldsProjections;
		}

		@Override
		public Object convertHit(JsonObject hit, ConversionContext conversionContext) {
			return convertFieldValue( hit, conversionContext );
		}

		private Object convertFieldValue(JsonObject hit, ConversionContext conversionContext) {
			Document tmp = new Document();

			defaultFieldProjection.addDocumentField( tmp, hit, conversionContext );

			// Add to the document the additional fields created when indexing the value
			for ( PrimitiveProjection subProjection : bridgeDefinedFieldsProjections ) {
				subProjection.addDocumentField( tmp, hit, conversionContext );
			}

			return conversionContext.twoWayConversionContext( bridge ).get( absoluteName, tmp );
		}
	}

	private static class PrimitiveProjection extends FieldProjection {
		private final TypeMetadata rootTypeMetadata;
		private final String absoluteName;
		private final ExtendedFieldType fieldType;

		public PrimitiveProjection(TypeMetadata rootTypeMetadata, String absoluteName, ExtendedFieldType fieldType) {
			super();
			this.rootTypeMetadata = rootTypeMetadata;
			this.absoluteName = absoluteName;
			this.fieldType = fieldType;
		}

		public void addDocumentField(Document tmp, JsonObject hit, ConversionContext conversionContext) {
			JsonElement jsonValue = extractFieldValue( hit.get( "_source" ).getAsJsonObject(), absoluteName );
			if ( jsonValue == null || jsonValue.isJsonNull() ) {
				return;
			}
			switch ( fieldType ) {
				case INTEGER:
					tmp.add( new IntField( absoluteName, jsonValue.getAsInt(), Store.NO ) );
					break;
				case LONG:
					tmp.add( new LongField( absoluteName, jsonValue.getAsLong(), Store.NO ) );
					break;
				case FLOAT:
					tmp.add( new FloatField( absoluteName, jsonValue.getAsFloat(), Store.NO ) );
					break;
				case DOUBLE:
					tmp.add( new DoubleField( absoluteName, jsonValue.getAsDouble(), Store.NO ) );
					break;
				case UNKNOWN_NUMERIC:
					throw LOG.unexpectedNumericEncodingType( rootTypeMetadata.getType().getName(), absoluteName );
				case BOOLEAN:
					tmp.add( new StringField( absoluteName, String.valueOf( jsonValue.getAsBoolean() ), Store.NO ) );
					break;
				default:
					tmp.add( new StringField( absoluteName, jsonValue.getAsString(), Store.NO ) );
					break;
			}
		}

		@Override
		public Object convertHit(JsonObject hit, ConversionContext conversionContext) {
			JsonElement jsonValue = extractFieldValue( hit.get( "_source" ).getAsJsonObject(), absoluteName );
			if ( jsonValue == null || jsonValue.isJsonNull() ) {
				return null;
			}
			switch ( fieldType ) {
				case INTEGER:
					return jsonValue.getAsInt();
				case LONG:
					return jsonValue.getAsLong();
				case FLOAT:
					return jsonValue.getAsFloat();
				case DOUBLE:
					return jsonValue.getAsDouble();
				case UNKNOWN_NUMERIC:
					throw LOG.unexpectedNumericEncodingType( rootTypeMetadata.getType().getName(), absoluteName );
				case BOOLEAN:
					return jsonValue.getAsBoolean();
				default:
					return jsonValue.getAsString();
			}
		}
	}

	private static class JsonDrivenProjection extends FieldProjection {
		private final String absoluteName;

		public JsonDrivenProjection(String absoluteName) {
			super();
			this.absoluteName = absoluteName;
		}

		@Override
		public Object convertHit(JsonObject hit, ConversionContext conversionContext) {
			JsonElement value = extractFieldValue( hit.get( "_source" ).getAsJsonObject(), absoluteName );
			if ( value == null || value.isJsonNull() ) {
				return null;
			}

			// TODO: HSEARCH-2255 should we do it?
			if ( !value.isJsonPrimitive() ) {
				throw LOG.unsupportedProjectionOfNonJsonPrimitiveFields( value );
			}

			JsonPrimitive primitive = value.getAsJsonPrimitive();

			if ( primitive.isBoolean() ) {
				return primitive.getAsBoolean();
			}
			else if ( primitive.isNumber() ) {
				// TODO HSEARCH-2255 this will expose a Gson-specific Number implementation; Can we somehow return an Integer,
				// Long... etc. instead?
				return primitive.getAsNumber();
			}
			else if ( primitive.isString() ) {
				return primitive.getAsString();
			}
			else {
				// TODO HSEARCH-2255 Better raise an exception?
				return primitive.toString();
			}
		}
	}
	/**
	 * A projection used whenever a given type has a one-way field bridge, which is forbidden.
	 *
	 * @author Yoann Rodiere
	 */
	private static class FailingOneWayFieldBridgeProjection extends FieldProjection {
		private final String absoluteName;
		private final Class<?> fieldBridgeClass;

		public FailingOneWayFieldBridgeProjection(String absoluteName, Class<?> fieldBridgeClass) {
			super();
			this.absoluteName = absoluteName;
			this.fieldBridgeClass = fieldBridgeClass;
		}

		@Override
		public Object convertHit(JsonObject hit, ConversionContext conversionContext) {
			throw LOG.projectingFieldWithoutTwoWayFieldBridge( absoluteName, fieldBridgeClass );
		}
	}

	@Override
	protected void extractFacetResults() {
		SearchResult searchResult = getSearchResult();
		JsonElement aggregationsElement = searchResult.getJsonObject().get( "aggregations" );
		if ( aggregationsElement == null ) {
			return;
		}
		JsonObject aggregations = aggregationsElement.getAsJsonObject();

		Map<String, List<Facet>> results = new HashMap<>();
		for ( FacetingRequest facetRequest : getFacetManager().getFacetRequests().values() ) {
			List<Facet> facets;
			if ( facetRequest instanceof DiscreteFacetRequest ) {
				facets = updateStringFacets( aggregations, (DiscreteFacetRequest) facetRequest );
				// Discrete facets are sorted by Elasticsearch
			}
			else {
				facets = updateRangeFacets( aggregations, (RangeFacetRequest<?>) facetRequest );
				if ( !FacetSortOrder.RANGE_DEFINITION_ORDER.equals( facetRequest.getSort() ) ) {
					Collections.sort( facets, FacetComparators.get( facetRequest.getSort() ) );
				}
			}

			results.put( facetRequest.getFacetingName(), facets );
		}
		getFacetManager().setFacetResults( results );
	}

	private List<Facet> updateRangeFacets(JsonObject aggregations, RangeFacetRequest<?> facetRequest) {
		if ( !ReflectionHelper.isIntegerType( facetRequest.getFacetValueType() )
				&& !Date.class.isAssignableFrom( facetRequest.getFacetValueType() )
				&& !ReflectionHelper.isFloatingPointType( facetRequest.getFacetValueType() ) ) {
			throw LOG.unsupportedFacetRangeParameter( facetRequest.getFacetValueType().getName() );
		}

		ArrayList<Facet> facets = new ArrayList<>();
		for ( FacetRange<?> facetRange : facetRequest.getFacetRangeList() ) {
			JsonElement aggregation = aggregations.get( facetRequest.getFacetingName() + "-" + facetRange.getIdentifier() );
			if ( aggregation == null ) {
				continue;
			}
			int docCount = aggregation.getAsJsonObject().get( "doc_count" ).getAsInt();
			if ( docCount == 0 && !facetRequest.hasZeroCountsIncluded() ) {
				continue;
			}
			facets.add( facetRequest.createFacet( facetRange.getRangeString(), docCount ) );
		}
		return facets;
	}

	private List<Facet> updateStringFacets(JsonObject aggregations, DiscreteFacetRequest facetRequest) {
		JsonElement aggregation = aggregations.get( facetRequest.getFacetingName() );
		if ( aggregation == null ) {
			return Collections.emptyList();
		}

		// deal with nested aggregation for nested documents
		if ( isNested( facetRequest ) ) {
			aggregation = aggregation.getAsJsonObject().get( facetRequest.getFacetingName() );
		}
		if ( aggregation == null ) {
			return Collections.emptyList();
		}

		ArrayList<Facet> facets = new ArrayList<>();
		for ( JsonElement bucket : aggregation.getAsJsonObject().get( "buckets" ).getAsJsonArray() ) {
			facets.add( facetRequest.createFacet(
					bucket.getAsJsonObject().get( "key" ).getAsString(),
					bucket.getAsJsonObject().get( "doc_count" ).getAsInt() ) );
		}
		return facets;
	}

	private JsonObject buildFullTextFilter(FullTextFilterImpl fullTextFilter) {

		/*
		 * FilterKey implementations and Filter(Factory) do not have to be threadsafe wrt their parameter injection
		 * as FilterCachingStrategy ensure a memory barrier between concurrent thread calls
		 */
		FilterDef def = extendedIntegrator.getFilterDefinition( fullTextFilter.getName() );
		//def can never be null, it's guarded by enableFullTextFilter(String)

		if ( isPreQueryFilterOnly( def ) ) {
			return null;
		}

		Object filterOrFactory = createFilterInstance( fullTextFilter, def );
		return createFullTextFilter( def, filterOrFactory );
	}

	protected JsonObject createFullTextFilter(FilterDef def, Object filterOrFactory) {
		JsonObject jsonFilter;
		if ( def.getFactoryMethod() != null ) {
			try {
				Object candidateFilter = def.getFactoryMethod().invoke( filterOrFactory );
				if ( candidateFilter instanceof Filter ) {
					jsonFilter = ToElasticsearch.fromLuceneFilter( (Filter) candidateFilter );
				}
				else if ( candidateFilter instanceof ElasticsearchFilter ) {
					jsonFilter = JSON_PARSER.parse( ( (ElasticsearchFilter) candidateFilter ).getJsonFilter() )
							.getAsJsonObject();
				}
				else {
					throw LOG.filterFactoryMethodReturnsUnsupportedType( def.getImpl().getName(), def.getFactoryMethod().getName() );
				}
			}
			catch (IllegalAccessException | InvocationTargetException e) {
				throw LOG.filterFactoryMethodInaccessible( def.getImpl().getName(), def.getFactoryMethod().getName(), e );
			}
		}
		else {
			if ( filterOrFactory instanceof Filter ) {
				jsonFilter = ToElasticsearch.fromLuceneFilter( (Filter) filterOrFactory );
			}
			else if ( filterOrFactory instanceof ElasticsearchFilter ) {
				jsonFilter = JSON_PARSER.parse( ( (ElasticsearchFilter) filterOrFactory ).getJsonFilter() ).getAsJsonObject();
			}
			else {
				throw LOG.filterHasUnsupportedType( filterOrFactory == null ? null : filterOrFactory.getClass().getName() );
			}
		}

		return jsonFilter;
	}

	private boolean isNested(DiscreteFacetRequest facetRequest) {
		//TODO HSEARCH-2097 Drive through meta-data
//		return FieldHelper.isEmbeddedField( facetRequest.getFieldName() );
		return false;
	}

	private boolean isPartOfProjectedFields(String projectionName) {
		if ( projectedFields == null ) {
			return false;
		}
		for ( String projectedField : projectedFields ) {
			if ( projectionName.equals( projectedField ) ) {
				return true;
			}
		}
		return false;
	}

	private class ElasticsearchScrollAPIDocumentExtractor implements DocumentExtractor {

		// Search parameters
		private final IndexSearcher searcher;
		private final Integer queryIndexLimit;

		// Position
		private String scrollId;

		// Results
		private Integer totalResultCount;
		private final Window<EntityInfo> results;

		private ElasticsearchScrollAPIDocumentExtractor() {
			searcher = new IndexSearcher();
			queryIndexLimit = ElasticsearchHSQueryImpl.this.maxResults == null
					? null : ElasticsearchHSQueryImpl.this.firstResult + ElasticsearchHSQueryImpl.this.maxResults;
			results = new Window<>(
					/*
					 * The offset is currently ignored by Elasticsearch.
					 * See https://github.com/elastic/elasticsearch/issues/9373
					 * To work this around, we don't use the "from" parameter when querying, and the document
					 * extractor will skip the results by querying until it gets the right index.
					 */
					0,
					/*
					 * Sizing for the worst-case scenario: we just fetched a batch of elements to
					 * give access to the result just after the previously fetched results, and
					 * we still need to keep enough of the previous elements to backtrack.
					 */
					searcher.getScrollBacktrackingWindowSize() + searcher.getScrollFetchSize()
					);
		}

		@Override
		public EntityInfo extract(int index) throws IOException {
			if ( index < 0 ) {
				throw new IndexOutOfBoundsException( "Index must be >= 0" );
			}
			else if ( index < results.start() ) {
				throw LOG.backtrackingWindowOverflow( searcher.getScrollBacktrackingWindowSize(), results.start(), index );
			}

			if ( totalResultCount == null ) {
				initResults();
			}

			int maxIndex = getMaxIndex();
			if ( maxIndex < index ) {
				throw new IndexOutOfBoundsException( "Index must be <= " + maxIndex );
			}

			boolean fetchMayReturnResults = true;
			while ( results.start() + results.size() <= index && fetchMayReturnResults ) {
				fetchMayReturnResults = fetchNextResults();
			}

			return results.get( index );
		}

		@Override
		public int getFirstIndex() {
			return ElasticsearchHSQueryImpl.this.firstResult;
		}

		@Override
		public int getMaxIndex() {
			if ( totalResultCount == null ) {
				initResults();
			}

			if ( queryIndexLimit == null ) {
				return totalResultCount - 1;
			}
			else {
				return Math.min( totalResultCount, queryIndexLimit ) - 1;
			}
		}

		@Override
		public void close() {
			if ( scrollId != null ) {
				searcher.clearScroll( scrollId );
				scrollId = null;
				totalResultCount = null;
				results.clear();
			}
		}

		@Override
		public TopDocs getTopDocs() {
			throw LOG.documentExtractorTopDocsUnsupported();
		}

		private void initResults() {
			SearchResult searchResult = searcher.searchWithScrollEnabled();
			totalResultCount = searchResult.getTotal();

			JsonObject searchResultJsonObject = searchResult.getJsonObject();
			extractWindow( searchResultJsonObject );
		}

		/**
		 * @return {@code true} if at least one result was fetched, {@code false} otherwise.
		 */
		private boolean fetchNextResults() {
			if ( totalResultCount <= results.start() + results.size() ) {
				// No more results to fetch
				return false;
			}

			JestResult searchResult = searcher.scroll( scrollId );
			JsonObject searchResultJsonObject = searchResult.getJsonObject();
			return extractWindow( searchResultJsonObject );
		}

		/**
		 * @return {@code true} if at least one result was fetched, {@code false} otherwise.
		 */
		private boolean extractWindow(JsonObject searchResultJsonObject) {
			boolean fetchedAtLeastOne = false;
			scrollId = searchResultJsonObject.get( "_scroll_id" ).getAsString();
			JsonArray hits = searchResultJsonObject.get( "hits" ).getAsJsonObject().get( "hits" ).getAsJsonArray();
			for ( JsonElement hit : hits ) {
				EntityInfo converted = searcher.convertQueryHit( searchResultJsonObject, hit.getAsJsonObject() );
				if ( converted != null ) {
					results.add( converted );
					fetchedAtLeastOne = true;
				}
			}
			return fetchedAtLeastOne;
		}
	}
}
