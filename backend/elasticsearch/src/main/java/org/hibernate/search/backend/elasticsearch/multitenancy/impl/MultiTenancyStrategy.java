/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.multitenancy.impl;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.common.impl.DocumentIdHelper;
import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.IndexSchemaRootContributor;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractionHelper;

import com.google.gson.JsonObject;

/**
 * Defines how the additional information required by multiTenancy are handled.
 */
public interface MultiTenancyStrategy {

	/**
	 * @return A schema contributor for the required additional properties (tenant ID, ...),
	 * or an empty optional.
	 */
	Optional<IndexSchemaRootContributor> indexSchemaRootContributor();

	/**
	 * @return A helper for creating predicates from tenant IDs.
	 */
	DocumentIdHelper documentIdHelper();

	/**
	 * @return A metadata contributor for the required additional properties (tenant ID, ...),
	 * or an empty optional.
	 */
	Optional<DocumentMetadataContributor> documentMetadataContributor();

	/**
	 * Generate a filter for the given tenant ID, to be applied to search queries.
	 *
	 * @param tenantId The tenant id.
	 * @return The filter, or {@code null} if no filter is necessary.
	 */
	JsonObject filterOrNull(String tenantId);

	/**
	 * Generate a filter for the given tenant IDs, to be applied to search queries.
	 *
	 * @param tenantIds The set of tenant ids.
	 * @return The filter, or {@code null} if no filter is necessary.
	 */
	JsonObject filterOrNull(Set<String> tenantIds);

	/**
	 * @return A helper for projections that need to extract the document id from search hits.
	 */
	ProjectionExtractionHelper<String> idProjectionExtractionHelper();
}
