/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
