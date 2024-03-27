/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.common.impl;

import java.util.Set;

import org.hibernate.search.util.common.reporting.EventContext;

public interface DocumentIdHelper {

	/**
	 * Check that the tenant id value is valid.
	 *
	 * @param tenantId The tenant id.
	 * @param context The context to add to exceptions (if any).
	 */
	void checkTenantId(String tenantId, EventContext context);

	/**
	 * Check that the set of tenant id values is valid.
	 *
	 * @param tenantIds The set of tenant ids.
	 * @param context The context to add to exceptions (if any).
	 */
	void checkTenantId(Set<String> tenantIds, EventContext context);

	/**
	 * Converts the object id to an Elasticsearch id: in the case of discriminator-based multi-tenancy, the id of the
	 * object is not unique so we need to disambiguate it.
	 *
	 * @param tenantId The id of the tenant. Might be null if multiTenancy is disabled.
	 * @param id The id of the indexed object.
	 * @return The Elasticsearch id.
	 */
	String toElasticsearchId(String tenantId, String id);

}
