/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
