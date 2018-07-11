/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.elasticsearch.logging.impl;

import java.util.Collection;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.index.impl.ElasticsearchIndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.engine.logging.spi.FailureContext;
import org.hibernate.search.engine.logging.spi.SearchExceptionWithContext;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.SearchException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@MessageLogger(projectCode = "HSEARCH-ES")
public interface Log extends BasicLogger {

	// -----------------------
	// Pre-existing messages
	// -----------------------

	@LogMessage(level = Level.WARN)
	@Message(id = /*ES_BACKEND_MESSAGES_START_ID +*/ 73,
			value = "Hibernate Search will connect to Elasticsearch server '%1$s' with authentication over plain HTTP (not HTTPS)."
					+ " The password will be sent in clear text over the network."
	)
	void usingPasswordOverHttp(String serverUris);

	@LogMessage(level = Level.DEBUG)
	@Message(id = /*ES_BACKEND_MESSAGES_START_ID +*/ 82,
			value = "Executed Elasticsearch HTTP %s request to path '%s' with query parameters %s in %dms."
					+ " Response had status %d '%s'."
	)
	void executedRequest(String method, String path, Map<String, String> getParameters, long timeInMs,
			int responseStatusCode, String responseStatusMessage);

	@Message(id = /*ES_BACKEND_MESSAGES_START_ID +*/ 89,
			value = "Failed to parse Elasticsearch response. Status code was '%1$d', status phrase was '%2$s'." )
	SearchException failedToParseElasticsearchResponse(int statusCode, String statusPhrase, @Cause Exception cause);

	@LogMessage(level = Level.TRACE)
	@Message(id = /*ES_BACKEND_MESSAGES_START_ID +*/ 93,
			value = "Executed Elasticsearch HTTP %s request to path '%s' with query parameters %s in %dms."
					+ " Response had status %d '%s'. Request body: <%s>. Response body: <%s>"
	)
	void executedRequest(String method, String path, Map<String, String> getParameters, long timeInMs,
			int responseStatusCode, String responseStatusMessage,
			String requestBodyParts, String responseBody);

	// -----------------------
	// New messages
	// -----------------------

	@Message(id = 502, value = "A search query cannot target both an Elasticsearch index and other types of index."
			+ " First target was: '%1$s', other target was: '%2$s'" )
	SearchExceptionWithContext cannotMixElasticsearchSearchTargetWithOtherType(IndexSearchTargetBuilder firstTarget,
			ElasticsearchIndexManager otherTarget, @Param FailureContext context);

	@Message(id = 503, value = "A search query cannot target multiple Elasticsearch backends."
			+ " First target was: '%1$s', other target was: '%2$s'" )
	SearchExceptionWithContext cannotMixElasticsearchSearchTargetWithOtherBackend(IndexSearchTargetBuilder firstTarget,
			ElasticsearchIndexManager otherTarget, @Param FailureContext context);

	@Message(id = 504, value = "Unknown field '%1$s'." )
	SearchExceptionWithContext unknownFieldForSearch(String absoluteFieldPath, @Param FailureContext context);

	@Message(id = 505, value = "Multiple conflicting types for field '%1$s': '%2$s' vs. '%3$s'." )
	SearchExceptionWithContext conflictingFieldTypesForSearch(String absoluteFieldPath,
			ElasticsearchIndexSchemaFieldNode schemaNode1, ElasticsearchIndexSchemaFieldNode schemaNode2,
			@Param FailureContext context);

	@Message(id = 506, value = "The Elasticsearch extension can only be applied to objects"
			+ " derived from the Elasticsearch backend. Was applied to '%1$s' instead." )
	SearchException elasticsearchExtensionOnUnknownType(Object context);

	@Message(id = 507, value = "Unknown projections %1$s." )
	SearchExceptionWithContext unknownProjectionForSearch(Collection<String> projections, @Param FailureContext context);

	@Message(id = 508, value = "An Elasticsearch query cannot include search predicates built using a non-Elasticsearch search target."
			+ " Given predicate was: '%1$s'" )
	SearchException cannotMixElasticsearchSearchQueryWithOtherPredicates(SearchPredicate predicate);

	@Message(id = 509, value = "Field '%1$s' is not an object field." )
	SearchExceptionWithContext nonObjectFieldForNestedQuery(String absoluteFieldPath, @Param FailureContext context);

	@Message(id = 510, value = "Object field '%1$s' is not stored as nested." )
	SearchExceptionWithContext nonNestedFieldForNestedQuery(String absoluteFieldPath, @Param FailureContext context);

	@Message(id = 511, value = "An Elasticsearch query cannot include search sorts built using a non-Elasticsearch search target."
			+ " Given sort was: '%1$s'" )
	SearchException cannotMixElasticsearchSearchSortWithOtherSorts(SearchSort sort);

	@Message(id = 512, value = "An analyzer was set on field '%1$s' with type '%2$s', but fields of this type cannot be analyzed." )
	SearchExceptionWithContext cannotUseAnalyzerOnFieldType(String relativeName, DataType fieldType,
			@Param FailureContext context);

	@Message(id = 513, value = "A normalizer was set on field '%1$s' with type '%2$s', but fields of this type cannot be analyzed." )
	SearchExceptionWithContext cannotUseNormalizerOnFieldType(String relativeName, DataType fieldType,
			@Param FailureContext context);

	@Message(id = 514, value = "Index '%1$s' requires multi-tenancy but the backend does not support it in its current configuration.")
	SearchExceptionWithContext multiTenancyRequiredButNotSupportedByBackend(String indexName, @Param FailureContext context);

	@Message(id = 515, value = "Unknown multi-tenancy strategy '%1$s'.")
	SearchException unknownMultiTenancyStrategyConfiguration(String multiTenancyStrategy);

	@Message(id = 516, value = "Tenant identifier '%1$s' is provided, but multi-tenancy is disabled for this backend.")
	SearchExceptionWithContext tenantIdProvidedButMultiTenancyDisabled(String tenantId, @Param FailureContext context);

	@Message(id = 517, value = "Backend has multi-tenancy enabled, but no tenant identifier is provided.")
	SearchExceptionWithContext multiTenancyEnabledButNoTenantIdProvided(@Param FailureContext context);

	@Message(id = 518, value = "Attempt to unwrap the Elasticsearch low-level client to %1$s,"
			+ " but the client can only be unwrapped to %2$s." )
	SearchException clientUnwrappingWithUnkownType(Class<?> requestedClass, Class<?> actualClass);

	@Message(id = 519, value = "Attempt to unwrap an Elasticsearch backend to %1$s,"
			+ " but this backend can only be unwrapped to %2$s." )
	SearchExceptionWithContext backendUnwrappingWithUnknownType(Class<?> requestedClass, Class<?> actualClass,
			@Param FailureContext context);

	@Message(id = 520, value = "The index schema node '%1$s' was added twice."
			+ " Multiple bridges may be trying to access the same index field, "
			+ " or two indexed-embeddeds may have prefixes that lead to conflicting field names,"
			+ " or you may have declared multiple conflicting mappings."
			+ " In any case, there is something wrong with your mapping and you should fix it." )
	SearchExceptionWithContext indexSchemaNodeNameConflict(String name,
			@Param FailureContext context);

	@Message(id = 523, value = "Range predicates are not supported by the GeoPoint field type, use spatial predicates instead.")
	SearchExceptionWithContext rangePredicatesNotSupportedByGeoPoint(@Param FailureContext context);

	@Message(id = 524, value = "Match predicates are not supported by the GeoPoint field type, use spatial predicates instead.")
	SearchExceptionWithContext matchPredicatesNotSupportedByGeoPoint(@Param FailureContext context);

	@Message(id = 525, value = "Invalid parent object for this field accessor; expected path '%1$s', got '%2$s'.")
	SearchException invalidParentDocumentObjectState(String expectedPath, String actualPath);

	@Message(id = 526, value = "Expected data was missing in the Elasticsearch response.")
	AssertionFailure elasticsearchResponseMissingData();

	@Message(id = 527, value = "Spatial predicates are not supported by this field's type.")
	SearchExceptionWithContext spatialPredicatesNotSupportedByFieldType(@Param FailureContext context);

	@Message(id = 528, value = "Distance related operations are not supported by this field's type.")
	SearchExceptionWithContext distanceOperationsNotSupportedByFieldType(@Param FailureContext context);

	@Message(id = 529, value = "Multiple conflicting minimumShouldMatch constraints for ceiling '%1$s'")
	SearchException minimumShouldMatchConflictingConstraints(int ceiling);

	@Message(id = 530, value = "Duplicate index names when normalized to conform to Elasticsearch rules:"
			+ " '%1$s' and '%2$s' both become '%3$s'")
	SearchExceptionWithContext duplicateNormalizedIndexNames(String firstHibernateSearchIndexName,
			String secondHibernateSearchIndexName, String elasticsearchIndexName,
			@Param FailureContext context);

	@Message(id = 531, value = "Unknown index name encountered in Elasticsearch response: '%1$s'")
	SearchExceptionWithContext elasticsearchResponseUnknownIndexName(String elasticsearchIndexName,
			@Param FailureContext context);
}
