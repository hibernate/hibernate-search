/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.multitenancy.impl;

import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.RootTypeMapping;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.JsonObject;

/**
 * Defines how the additional information required by multiTenancy are handled.
 */
public interface MultiTenancyStrategy {

	/**
	 * Indicates if the strategy supports multiTenancy.
	 *
	 * @return {@code true} if multiTenancy is supported, {@code false} otherwise.
	 */
	boolean isMultiTenancySupported();

	/**
	 * Contributes the additional properties to the Elasticsearch schema.
	 *
	 * @param rootTypeMapping The root type mapping.
	 */
	void contributeToMapping(RootTypeMapping rootTypeMapping);

	/**
	 * Converts the object id to an Elasticsearch id: in the case of discriminator-based multi-tenancy, the id of the
	 * object is not unique so we need to disambiguate it.
	 *
	 * @param tenantId The id of the tenant. Might be null if multiTenancy is disabled.
	 * @param id The id of the indexed object.
	 * @return The Elasticsearch id.
	 */
	String toElasticsearchId(String tenantId, String id);

	/**
	 * Contributes additional information to the indexed document.
	 *
	 * @param document The indexed document.
	 * @param tenantId The tenant id.
	 * @param id The object id.
	 */
	void contributeToIndexedDocument(JsonObject document, String tenantId, String id);

	/**
	 * Decorate the query with the tenant constraint.
	 *
	 * @param originalJsonQuery The original JSON query.
	 * @param tenantId The tenant id.
	 * @return The decorated query.
	 */
	JsonObject decorateJsonQuery(JsonObject originalJsonQuery, String tenantId);

	/**
	 * Contributes additional elements to the Elasticsearch search request.
	 *
	 * @param requestBody The body of the request.
	 */
	void contributeToSearchRequest(JsonObject requestBody);

	/**
	 * Extracts the tenant-scoped document id from an Elasticsearch hit.
	 *
	 * @param hit The Elasticsearch hit.
	 * @return The tenant scoped document id.
	 */
	String extractTenantScopedDocumentId(JsonObject hit);

	/**
	 * Check that the tenant id value is consistent with the strategy.
	 *
	 * @param tenantId The tenant id.
	 * @param backendContext The backend.
	 */
	void checkTenantId(String tenantId, EventContext backendContext);
}
