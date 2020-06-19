/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.common.impl;

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
	 * Converts the object id to an Elasticsearch id: in the case of discriminator-based multi-tenancy, the id of the
	 * object is not unique so we need to disambiguate it.
	 *
	 * @param tenantId The id of the tenant. Might be null if multiTenancy is disabled.
	 * @param id The id of the indexed object.
	 * @return The Elasticsearch id.
	 */
	String toElasticsearchId(String tenantId, String id);

}
