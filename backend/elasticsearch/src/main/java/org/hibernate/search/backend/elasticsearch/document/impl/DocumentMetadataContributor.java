package org.hibernate.search.backend.elasticsearch.document.impl;

import tools.jackson.databind.node.ObjectNode;

public interface DocumentMetadataContributor {

	/**
	 * Contributes metadata to the indexed document.
	 *
	 * @param document The indexed document.
	 * @param tenantId The tenant id.
	 * @param id The document id.
	 */
	void contribute(ObjectNode document, String tenantId, String id);
}
