/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import com.google.gson.JsonObject;

public interface DocumentMetadataContributor {

	/**
	 * Contributes metadata to the indexed document.
	 *
	 * @param document The indexed document.
	 * @param tenantId The tenant id.
	 * @param id The document id.
	 */
	void contribute(JsonObject document, String tenantId, String id);

}
