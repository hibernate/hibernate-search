/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.cfg;

/**
 * Configuration values for Elasticsearch "dynamic" attribute values.
 * <p>
 * Defines the behaviour when a document contains a field the wasn't define in the index schema.
 * <p>
 * More details on the Elasticsearch documentation:
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/dynamic.html">Elasticsearch mapping:
 * dynamic</a>
 *
 * @author Davide D'Alto
 */
public enum DynamicType {

	/**
	 * Add unrecognized fields to the schema
	 */
	TRUE,

	/**
	 * Ignore unrecognized field
	 */
	FALSE,

	/**
	 * Throw exception when the field is unrecognized
	 */
	STRICT;
}
