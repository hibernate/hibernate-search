/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl.model;

/**
 * An enum for Elasticsearch "index" attribute values.
 * <p>
 * Serialized using specific adapters for each version of ES.
 * <p>
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-index.html
 *
 * @author Yoann Rodiere
 */
public enum IndexType {

	/**
	 * @deprecated ES2 only.
	 */
	@Deprecated
	NOT_ANALYZED,
	/**
	 * @deprecated ES2 only.
	 */
	@Deprecated
	ANALYZED,
	/**
	 * @deprecated ES2 only.
	 */
	@Deprecated
	NO,
	TRUE,
	FALSE
	;
}
