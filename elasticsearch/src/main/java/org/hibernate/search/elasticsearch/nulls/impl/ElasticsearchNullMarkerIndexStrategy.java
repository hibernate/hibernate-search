/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.nulls.impl;


/**
 * @author Yoann Rodiere
 */
public enum ElasticsearchNullMarkerIndexStrategy {

	/**
	 * Do <strong>not</strong> index null markers derived from the "indexNullAs" string,
	 * but index null instead, because null_value is used in the
	 * Elasticsearch schema.
	 */
	DEFAULT,
	/**
	 * Do index null markers derived from the "indexNullAs" string,
	 * because null_value is <strong>not</strong> used in the Elasticsearch schema.
	 */
	CONTAINER;

}
