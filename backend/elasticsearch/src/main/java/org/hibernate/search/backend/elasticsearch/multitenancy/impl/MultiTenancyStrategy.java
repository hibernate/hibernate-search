/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.multitenancy.impl;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.IndexSchemaRootContributor;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractionHelper;
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
	 * @return A schema contributor for the required additional properties (tenant ID, ...),
	 * or an empty optional.
	 */
	Optional<IndexSchemaRootContributor> getIndexSchemaRootContributor();

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
	 * @return A metadata contributor for the required additional properties (tenant ID, ...),
	 * or an empty optional.
	 */
	Optional<DocumentMetadataContributor> getDocumentMetadataContributor();

	/**
	 * Decorate the query with the tenant constraint.
	 *
	 * @param originalJsonQuery The original JSON query.
	 * @param tenantId The tenant id.
	 * @return The decorated query.
	 */
	JsonObject decorateJsonQuery(JsonObject originalJsonQuery, String tenantId);

	/**
	 * @return A helper for projections that need to extract the document id from search hits.
	 */
	ProjectionExtractionHelper<String> getIdProjectionExtractionHelper();

	/**
	 * Check that the tenant id value is consistent with the strategy.
	 *
	 * @param tenantId The tenant id.
	 * @param backendContext The backend.
	 */
	void checkTenantId(String tenantId, EventContext backendContext);
}
