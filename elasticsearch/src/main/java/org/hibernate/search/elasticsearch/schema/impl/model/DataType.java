/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl.model;

import com.google.gson.annotations.SerializedName;

/**
 * An enum for Elasticsearch data types.
 * <p>Only provides values for the types we actually use.
 *
 * @see https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-types.html
 * @author Yoann Rodiere
 */
public enum DataType {

	@SerializedName("object")
	OBJECT,
	@SerializedName("string")
	STRING,
	@SerializedName("long")
	LONG,
	@SerializedName("integer")
	INTEGER,
	@SerializedName("double")
	DOUBLE,
	@SerializedName("float")
	FLOAT,
	@SerializedName("date")
	DATE,
	@SerializedName("boolean")
	BOOLEAN,
	@SerializedName("geo_point")
	GEO_POINT
	;
}
