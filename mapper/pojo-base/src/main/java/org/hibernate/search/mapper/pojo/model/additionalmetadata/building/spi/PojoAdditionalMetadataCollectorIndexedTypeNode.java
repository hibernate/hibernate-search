/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;

public interface PojoAdditionalMetadataCollectorIndexedTypeNode extends PojoAdditionalMetadataCollector {

	/**
	 * @param backendName The name of the backend where this type should be indexed,
	 * or {@code null} (the default) to target the default backend.
	 */
	void backendName(String backendName);

	/**
	 * @param indexName The name of the backend where this type should be indexed,
	 * or {@code null} (the default) to derive the index name from the entity type.
	 */
	void indexName(String indexName);

	/**
	 * @param enabled {@code true} if this type must be indexed
	 * (the default once a {@link PojoAdditionalMetadataCollectorIndexedTypeNode} is created),
	 * {@code false} if it must not (in which case metadata provided through other methods is ignored).
	 */
	void enabled(boolean enabled);

	/**
	 * @param binder The routing binder.
	 * @param params The parameters to pass to the binder.
	 */
	void routingBinder(RoutingBinder binder, Map<String, Object> params);

}
