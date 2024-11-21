/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.multitenancy.impl;

import java.util.Set;

import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;

/**
 * Defines how the additional information required by multiTenancy are handled.
 */
public interface MultiTenancyStrategy {

	/**
	 * Contributes additional information to the indexed document.
	 *
	 * @param document The indexed document.
	 * @param tenantId The tenant id.
	 */
	void contributeToIndexedDocument(Document document, String tenantId);

	/**
	 * Generate a filter for the given tenant ID, to be applied to search queries
	 * and update/delete operations.
	 *
	 * @param tenantId The tenant id.
	 * @return The filter, or {@code null} if no filter is necessary.
	 */
	Query filterOrNull(String tenantId);

	/**
	 * Generate a filter for the given set of tenant IDs, to be applied to search queries.
	 *
	 * @param tenantIds The set of tenant ids.
	 * @return The filter, or {@code null} if no filter is necessary.
	 */
	Query filterOrNull(Set<String> tenantIds);

	/**
	 * Check that the tenant id value is consistent with the strategy.
	 *
	 * @param tenantId The tenant id.
	 * @param backendContext The backend.
	 */
	void checkTenantId(String tenantId, EventContext backendContext);

	/**
	 * Check that the set of tenant id values is valid.
	 *
	 * @param tenantIds The set of tenant ids.
	 * @param context The context to add to exceptions (if any).
	 */
	void checkTenantId(Set<String> tenantIds, EventContext context);

}
