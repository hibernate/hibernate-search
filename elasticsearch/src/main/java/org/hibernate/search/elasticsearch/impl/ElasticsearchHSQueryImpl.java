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
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.elasticsearch.ElasticsearchProjectionConstants;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.filter.ElasticsearchFilter;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper.ExtendedFieldType;
import org.hibernate.search.elasticsearch.util.impl.Window;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ExplainResult;
import org.hibernate.search.elasticsearch.work.impl.SearchResult;
import org.hibernate.search.elasticsearch.work.impl.builder.SearchWorkBuilder;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.BridgeDefinedField;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.FacetMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.AssertionFailure;
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
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

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
		IndexSearcher searcher = getOrCreateSearcher();
		if ( searcher != null ) {
			return new ElasticsearchScrollAPIDocumentExtractor( searcher );
		}
		else {
			return EmptyDocumentExtractor.get();
		}
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

		JsonObject hit = searchResult.getHits()
				// TODO Is it right to use the document id that way? I am not quite clear about its semantics
				.get( documentId )
				.getAsJsonObject();

		/*
		 * Do not add every property of the original payload: some properties, such as "_source", do not have the same syntax
		 * and are not necessary to the explanation.
		 */
		JsonObject explainPayload = JsonBuilder.object()
				.add( "query", searcher.payload.get( "query" ) )
				.build();

		ElasticsearchWork<ExplainResult> work = searcher.elasticsearchService.getWorkFactory().explain(
				URLEncodedString.fromJSon( hit.get( "_index" ) ),
				URLEncodedString.fromJSon( hit.get( "_type" ) ),
				URLEncodedString.fromJSon( hit.get( "_id" ) ),
				explainPayload )
				.build();

		ExplainResult result = searcher.elasticsearchService.getWorkProcessor().executeSyncUnsafe( work );
		JsonObject explanation = result.getJsonObject().get( "explanation" ).getAsJsonObject();

		return convertExplanation( explanation );
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

		JsonArray hits = searchResult.getHits();
		List<EntityInfo> results = new ArrayList<>( hits.size() );

		for ( JsonElement hit : hits ) {
			EntityInfo entityInfo = searcher.convertQueryHit( searchResult, hit.getAsJsonObject() );
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
		IndexSearcher searcher = getOrCreateSearcher();
		if ( searcher != null ) {
			searchResult = searcher.search();
		}
		else {
			searchResult = EmptySearchResult.get();
		}
		resultSize = searchResult.getTotalHitCount();
	}

	private IndexSearcher getOrCreateSearcher() {
		if ( searcher != null ) {
			return searcher;
		}

		ElasticsearchService elasticsearchService = null;
		Map<String, EntityIndexBinding> targetedEntityBindingsByName = buildTargetedEntityIndexBindingsByName();
		Set<URLEncodedString> indexNames = new HashSet<>();

		for ( Map.Entry<String, EntityIndexBinding> entry: targetedEntityBindingsByName.entrySet() ) {
			EntityIndexBinding binding = entry.getValue();

			IndexManager[] indexManagers = binding.getIndexManagers();

			for ( IndexManager indexManager : indexManagers ) {
				if ( !( indexManager instanceof ElasticsearchIndexManager ) ) {
					throw LOG.cannotRunEsQueryTargetingEntityIndexedWithNonEsIndexManager(
						binding.getDocumentBuilder().getBeanClass(),
						rawSearchPayload.toString()
					);
				}

				ElasticsearchIndexManager esIndexManager = (ElasticsearchIndexManager) indexManager;
				indexNames.add( URLEncodedString.fromString( esIndexManager.getActualIndexName() ) );
				if ( elasticsearchService == null ) {
					elasticsearchService = esIndexManager.getElasticsearchService();
				}
				else if ( elasticsearchService != esIndexManager.getElasticsearchService() ) {
					throw new AssertionFailure( "Found two index managers refering to two different ElasticsearchService instances" );
				}
			}
		}

		if ( indexNames.isEmpty() ) {
			/*
			 * In this case we cannot send a request to Elasticsearch,
			 * because by default it will query all indexes.
			 */
			return null;
		}

		this.searcher = new IndexSearcher( elasticsearchService, targetedEntityBindingsByName, indexNames );
		return searcher;
	}

	/**
	 * Stores all information required to execute the query: query string, relevant indices, query parameters, ...
	 */
	private class IndexSearcher {
		private final ElasticsearchService elasticsearchService;
		private final Map<String, EntityIndexBinding> targetedEntityBindingsByName;
		private final Set<URLEncodedString> indexNames;

		private final ElasticsearchQueryOptions queryOptions;
		private final Map<EntityIndexBinding, FieldProjection> idProjectionByEntityBinding = new HashMap<>();
		private final Map<EntityIndexBinding, FieldProjection[]> fieldProjectionsByEntityBinding = new HashMap<>();
		private final Map<FacetingRequest, FacetMetadata> facetingRequestsAndMetadata;
		private final JsonObject payload;

		private IndexSearcher(ElasticsearchService elasticsearchService,
				Map<String, EntityIndexBinding> targetedEntityBindingsByName, Set<URLEncodedString> indexNames) {
			this.elasticsearchService = elasticsearchService;
			this.targetedEntityBindingsByName = targetedEntityBindingsByName;
			this.indexNames = indexNames;

			JsonArray typeFilters = new JsonArray();
			for ( String typeName : targetedEntityBindingsByName.keySet() ) {
				typeFilters.add( getEntityTypeFilter( typeName ) );
			}

			this.queryOptions = elasticsearchService.getQueryOptions();

			// Query filters; always a type filter, possibly a tenant id filter;
			JsonObject filteredQuery = getFilteredQuery( rawSearchPayload.get( "query" ), typeFilters );

			JsonBuilder.Object payloadBuilder = JsonBuilder.object();
			payloadBuilder.add( "query", filteredQuery );

			addProjections( payloadBuilder );

			Collection<FacetingRequest> facetingRequests = getFacetManager().getFacetRequests().values();
			if ( !facetingRequests.isEmpty() ) {
				JsonBuilder.Object facets = JsonBuilder.object();

				facetingRequestsAndMetadata =
						buildFacetingRequestsAndMetadata( facetingRequests, targetedEntityBindingsByName.values() );
				for ( Entry<FacetingRequest, FacetMetadata> facetingRequestEntry : facetingRequestsAndMetadata.entrySet() ) {
					addFacetingRequest( facets, facetingRequestEntry.getKey(), facetingRequestEntry.getValue() );
				}

				payloadBuilder.add( "aggregations", facets );
			}
			else {
				facetingRequestsAndMetadata = Collections.emptyMap();
			}

			// Initialize the sortByDistanceIndex to detect if the results are sorted by distance and the position
			// of the sort
			sortByDistanceIndex = getSortByDistanceIndex();
			addScriptFields( payloadBuilder );

			// TODO: HSEARCH-2254 embedded fields (see https://github.com/searchbox-io/Jest/issues/304)
			if ( sort != null ) {
				validateSortFields( targetedEntityBindingsByName.values() );
				payloadBuilder.add( "sort", ToElasticsearch.fromLuceneSort( sort ) );
			}

			this.payload = payloadBuilder.build();
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

		private JsonObject getEntityTypeFilter(String name) {
			JsonObject value = new JsonObject();
			value.addProperty( "value", name );

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

		private void addProjections(JsonBuilder.Object payloadBuilder) {
			boolean includeAllSource = false;
			JsonBuilder.Array builder = JsonBuilder.array();

			/*
			 * IDs are always projected: always initialize their projections regardless of the
			 * "projectedFields" attribute.
			 */
			for ( EntityIndexBinding binding : targetedEntityBindingsByName.values() ) {
				DocumentBuilderIndexedEntity documentBuilder = binding.getDocumentBuilder();
				String idFieldName = documentBuilder.getIdFieldName();
				TypeMetadata typeMetadata = documentBuilder.getTypeMetadata();
				FieldProjection projection = createProjection( builder, typeMetadata, idFieldName );
				idProjectionByEntityBinding.put( binding, projection );
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
							for ( EntityIndexBinding binding : targetedEntityBindingsByName.values() ) {
								TypeMetadata typeMetadata = binding.getDocumentBuilder().getTypeMetadata();
								FieldProjection projection = createProjection( builder, typeMetadata, projectedField );
								FieldProjection[] projectionsForType = fieldProjectionsByEntityBinding.get( binding );
								if ( projectionsForType == null ) {
									projectionsForType = new FieldProjection[projectedFields.length];
									fieldProjectionsByEntityBinding.put( binding, projectionsForType );
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

		private void addFacetingRequest(JsonBuilder.Object facets, FacetingRequest facetingRequest, FacetMetadata facetMetadata) {
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
								// We multiply by 0.001 to Convert from meters to kilmeters
								.addProperty( "inline", "doc['" + spatialFieldName + "'] ? doc['" + spatialFieldName + "'].arcDistance(lat,lon)*0.001 : null" )
								.addProperty( "lang", "groovy" )
							)
						)
				);
			}
		}

		SearchResult search() {
			return search( false );
		}

		SearchResult searchWithScrollEnabled() {
			return search( true );
		}

		private SearchResult search(boolean enableScrolling) {
			SearchWorkBuilder builder = elasticsearchService.getWorkFactory()
					.search( payload ).indexes( indexNames );

			if ( enableScrolling ) {
				/*
				 * Note: "firstResult" is currently being ignored by Elasticsearch when scrolling.
				 * See https://github.com/elastic/elasticsearch/issues/9373
				 * To work this around, we don't use the "from" parameter here, and the document
				 * extractor will skip the results by scrolling until it gets the right index.
				 */
				builder.scrolling(
						/*
						 * The "size" parameter has a slightly different meaning when scrolling: it defines the window
						 * size for the first search *and* for the next calls to the scroll API.
						 * We still reduce the number of results in accordance to "maxResults", but that's done on our side
						 * in the document extractor.
						 */
						getQueryOptions().getScrollFetchSize(),
						getQueryOptions().getScrollTimeout()
				);
			}
			else {
				builder.paging(
						firstResult,
						// If the user has given a 'size' value, take it as is, let ES itself complain if it's too high; if no value is
						// given, I take as much as possible, as by default only 10 rows would be returned
						maxResults != null ? maxResults : MAX_RESULT_WINDOW_SIZE - firstResult
				);
			}

			ElasticsearchWork<SearchResult> work = builder.build();
			return elasticsearchService.getWorkProcessor().executeSyncUnsafe( work );
		}

		private ElasticsearchQueryOptions getQueryOptions() {
			return queryOptions;
		}

		/**
		 * Scroll through search results, using a previously obtained scrollId.
		 */
		SearchResult scroll(String scrollId) {
			ElasticsearchQueryOptions queryOptions = getQueryOptions();
			ElasticsearchWork<SearchResult> work = elasticsearchService.getWorkFactory()
					.scroll( scrollId, queryOptions.getScrollTimeout() ).build();
			return elasticsearchService.getWorkProcessor().executeSyncUnsafe( work );
		}

		void clearScroll(String scrollId) {
			ElasticsearchWork<?> work = elasticsearchService.getWorkFactory()
					.clearScroll( scrollId ).build();
			elasticsearchService.getWorkProcessor().executeSyncUnsafe( work );
		}

		EntityInfo convertQueryHit(SearchResult searchResult, JsonObject hit) {
			String type = hit.get( "_type" ).getAsString();
			EntityIndexBinding binding = targetedEntityBindingsByName.get( type );

			if ( binding == null ) {
				LOG.warnf( "Found unknown type in Elasticsearch index: " + type );
				return null;
			}

			DocumentBuilderIndexedEntity documentBuilder = binding.getDocumentBuilder();
			Class<?> clazz = documentBuilder.getBeanClass();

			ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
			conversionContext.setClass( clazz );
			FieldProjection idProjection = idProjectionByEntityBinding.get( binding );
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
							if ( distance != null && distance.isJsonArray() ) {
								JsonArray array = distance.getAsJsonArray();
								distance = array.size() >= 1 ? array.get( 0 ) : null;
							}
							if ( distance == null || distance.isJsonNull() ) {
								projections[i] = null;
							}
							else {
								Double distanceAsDouble = distance.getAsDouble();

								if ( distanceAsDouble == Double.MAX_VALUE || distanceAsDouble.isInfinite() ) {
									/*
									 * When we extract the distance from the sort, its default value is:
									 *  - Double.MAX_VALUE on older ES versions (5.0 and lower)
									 *  - Double.POSITIVE_INFINITY on newer ES versions (from somewhere around 5.2 onwards)
									 */
									projections[i] = null;
								}
								else {
									projections[i] = distance.getAsDouble();
								}
							}
							break;
						case ElasticsearchProjectionConstants.TOOK:
							projections[i] = searchResult.getTook();
							break;
						case ElasticsearchProjectionConstants.TIMED_OUT:
							projections[i] = searchResult.getTimedOut();
							break;
						case ElasticsearchProjectionConstants.THIS:
							// Use EntityInfo.ENTITY_PLACEHOLDER as placeholder.
							// It will be replaced when we populate
							// the EntityInfo with the real entity.
							projections[i] = EntityInfo.ENTITY_PLACEHOLDER;
							break;
						default:
							FieldProjection projection = fieldProjectionsByEntityBinding.get( binding )[i];
							projections[i] = projection.convertHit( hit, conversionContext );
					}
				}
			}

			return new EntityInfoImpl( clazz, documentBuilder.getIdPropertyName(), (Serializable) id, projections );
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
		JsonObject aggregations = searchResult.getAggregations();
		if ( aggregations == null ) {
			return;
		}

		Map<String, List<Facet>> results = new HashMap<>();
		for ( Map.Entry<FacetingRequest, FacetMetadata> entry : searcher.facetingRequestsAndMetadata.entrySet() ) {
			FacetingRequest facetRequest = entry.getKey();
			FacetMetadata facetMetadata = entry.getValue();
			List<Facet> facets;
			if ( facetRequest instanceof DiscreteFacetRequest ) {
				facets = extractDiscreteFacets( aggregations, (DiscreteFacetRequest) facetRequest, facetMetadata );
				// Discrete facets are sorted by Elasticsearch
			}
			else {
				facets = extractRangeFacets( aggregations, (RangeFacetRequest<?>) facetRequest, facetMetadata );
				if ( !FacetSortOrder.RANGE_DEFINITION_ORDER.equals( facetRequest.getSort() ) ) {
					Collections.sort( facets, FacetComparators.get( facetRequest.getSort() ) );
				}
			}

			results.put( facetRequest.getFacetingName(), facets );
		}
		getFacetManager().setFacetResults( results );
	}

	private List<Facet> extractRangeFacets(JsonObject aggregations, RangeFacetRequest<?> facetRequest,
			FacetMetadata facetMetadata) {
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
			facets.add( facetRequest.createFacet( facetMetadata, facetRange.getRangeString(), docCount ) );
		}
		return facets;
	}

	private List<Facet> extractDiscreteFacets(JsonObject aggregations, DiscreteFacetRequest facetRequest,
			FacetMetadata facetMetadata) {
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
					facetMetadata,
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
				jsonFilter = toJsonFilter( candidateFilter );
				if ( jsonFilter == null ) {
					throw LOG.filterFactoryMethodReturnsUnsupportedType( def.getImpl().getName(), def.getFactoryMethod().getName() );
				}
			}
			catch (IllegalAccessException | InvocationTargetException e) {
				throw LOG.filterFactoryMethodInaccessible( def.getImpl().getName(), def.getFactoryMethod().getName(), e );
			}
		}
		else {
			jsonFilter = toJsonFilter( filterOrFactory );
			if ( jsonFilter == null ) {
				throw LOG.filterHasUnsupportedType( filterOrFactory == null ? null : filterOrFactory.getClass().getName() );
			}
		}

		return jsonFilter;
	}

	private JsonObject toJsonFilter(Object candidateFilter) {
		if ( candidateFilter instanceof Query ) {
			// This also handles the case where the query extends Filter
			return ToElasticsearch.fromLuceneQuery( (Query) candidateFilter );
		}
		else if ( candidateFilter instanceof ElasticsearchFilter ) {
			return JSON_PARSER.parse( ( (ElasticsearchFilter) candidateFilter ).getJsonFilter() ).getAsJsonObject();
		}
		else {
			return null;
		}
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

		private ElasticsearchScrollAPIDocumentExtractor(IndexSearcher searcher) {
			this.searcher = searcher;
			queryIndexLimit = ElasticsearchHSQueryImpl.this.maxResults == null
					? null : ElasticsearchHSQueryImpl.this.firstResult + ElasticsearchHSQueryImpl.this.maxResults;
			ElasticsearchQueryOptions queryOptions = searcher.getQueryOptions();
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
					queryOptions.getScrollBacktrackingWindowSize() + queryOptions.getScrollFetchSize()
					);
		}

		@Override
		public EntityInfo extract(int index) throws IOException {
			if ( index < 0 ) {
				throw new IndexOutOfBoundsException( "Index must be >= 0" );
			}
			else if ( index < results.start() ) {
				throw LOG.backtrackingWindowOverflow( searcher.getQueryOptions().getScrollBacktrackingWindowSize(), results.start(), index );
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
			totalResultCount = searchResult.getTotalHitCount();
			extractWindow( searchResult );
		}

		/**
		 * @return {@code true} if at least one result was fetched, {@code false} otherwise.
		 */
		private boolean fetchNextResults() {
			if ( totalResultCount <= results.start() + results.size() ) {
				// No more results to fetch
				return false;
			}

			SearchResult searchResult = searcher.scroll( scrollId );
			return extractWindow( searchResult );
		}

		/**
		 * @return {@code true} if at least one result was fetched, {@code false} otherwise.
		 */
		private boolean extractWindow(SearchResult searchResult) {
			boolean fetchedAtLeastOne = false;
			scrollId = searchResult.getScrollId();
			JsonArray hits = searchResult.getHits();
			for ( JsonElement hit : hits ) {
				EntityInfo converted = searcher.convertQueryHit( searchResult, hit.getAsJsonObject() );
				if ( converted != null ) {
					results.add( converted );
					fetchedAtLeastOne = true;
				}
			}
			return fetchedAtLeastOne;
		}
	}
}
