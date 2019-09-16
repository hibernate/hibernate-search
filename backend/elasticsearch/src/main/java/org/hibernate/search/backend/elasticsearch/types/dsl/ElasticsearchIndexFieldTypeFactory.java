/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl;

import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.dsl.sort.ElasticsearchSearchSortFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;


public interface ElasticsearchIndexFieldTypeFactory extends IndexFieldTypeFactory {

	/**
	 * Define a native field type.
	 * <p>
	 * A native field type has the following characteristics:
	 * <ul>
	 *     <li>Hibernate Search doesn't know its exact type, so it must be entirely defined as a JSON object,
	 *     provided as the {@code mappingJsonString} parameter</li>
	 *     <li>When indexing, fields of this type must be populated with JSON.
	 *     The field has a string type, but the string is interpreted as JSON, so it can contain a boolean, an integer,
	 *     or even an object.</li>
	 *     <li>The predicate/sort/projection DSLs have only limited support for fields of this type.
	 *     Some features may not work and throw an exception, such as phrase predicates.
	 *     It is recommended to create the predicate/sort/projections targeting these fields from JSON
	 *     using {@link ElasticsearchSearchPredicateFactory#fromJson(String)}
	 *     or {@link ElasticsearchSearchSortFactory#fromJson(String)}</li>
	 * </ul>
	 *
	 * @param mappingJsonString A string representing an Elasticsearch field mapping as a JSON object.
	 * The JSON object must be a syntactically correct Elasticsearch field mapping.
	 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html">the Elasticsearch documentation</a>.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	ElasticsearchNativeIndexFieldTypeOptionsStep<?> asNative(String mappingJsonString);

}
