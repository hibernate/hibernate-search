/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.work.execution.impl;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;

import com.google.gson.JsonObject;

/**
 * An interface with knowledge of the index manager internals,
 * able to leverage information necessary for work execution on this index.
 * <p>
 * Note this interface exists mainly to more cleanly pass information
 * from the index manager to the various work execution components.
 * If we just passed the index manager to the various work execution components,
 * we would have a cyclic dependency.
 * If we passed all the components held by the index manager to the various work execution components,
 * we would end up with methods with many parameters.
 */
public interface WorkExecutionIndexManagerContext {

	String getMappedTypeName();

	URLEncodedString getElasticsearchIndexWriteName();

	String toElasticsearchId(String tenantId, String id);

	JsonObject createDocument(String tenantId, String id,
			DocumentContributor documentContributor);

}
