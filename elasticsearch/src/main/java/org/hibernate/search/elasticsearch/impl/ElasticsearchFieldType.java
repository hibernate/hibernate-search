/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

/**
 * An enum representation of Elasticsearch field datatypes.
 *
 * <p>Only those types that are supported by Hibernate Search are listed.
 *
 * @author Yoann Rodiere
 */
enum ElasticsearchFieldType {

	STRING("string"),
	LONG("long"),
	INTEGER("integer"),
	DOUBLE("double"),
	FLOAT("float"),
	DATE("date"),
	BOOLEAN("boolean"),

	GEO_POINT("geo_point");

	private final String elasticsearchString;

	private ElasticsearchFieldType(String elasticsearchString) {
		this.elasticsearchString = elasticsearchString;
	}

	/**
	 * @return the Elasticsearch name for this type.
	 */
	public String getElasticsearchString() {
		return elasticsearchString;
	};

}
