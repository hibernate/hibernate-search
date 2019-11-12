/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl;

import com.google.gson.JsonObject;

public interface ElasticsearchNativeIndexFieldTypeMappingStep {

	/**
	 * @param jsonObject A {@link JsonObject} representing an Elasticsearch field mapping.
	 * The JSON object must be a syntactically correct Elasticsearch field mapping.
	 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html">the Elasticsearch documentation</a>.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	ElasticsearchNativeIndexFieldTypeOptionsStep<?> mapping(JsonObject jsonObject);

	/**
	 * @param jsonString A JSON-formatted string representing an Elasticsearch field mapping.
	 * The JSON object must be a syntactically correct Elasticsearch field mapping.
	 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html">the Elasticsearch documentation</a>.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	ElasticsearchNativeIndexFieldTypeOptionsStep<?> mapping(String jsonString);

}
