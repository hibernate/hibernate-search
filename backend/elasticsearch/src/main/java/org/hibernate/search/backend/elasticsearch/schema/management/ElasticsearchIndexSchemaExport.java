/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.schema.management;

import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.common.schema.management.SchemaExport;
import org.hibernate.search.util.common.annotation.Incubating;

import com.google.gson.JsonObject;

/**
 * Extended version of an {@link SchemaExport} that exposes any Elasticsearch-specific methods.
 */
@Incubating
public interface ElasticsearchIndexSchemaExport extends SchemaExport {

	/**
	 * @return The map containing any query parameters required to send a request to an Elasticsearch cluster to create the
	 * index this export represents.
	 */
	Map<String, String> parameters();

	/**
	 * @return The list containing any body elements required to send a request to an Elasticsearch cluster to create the
	 * index this export represents.
	 */
	List<JsonObject> bodyParts();

}
