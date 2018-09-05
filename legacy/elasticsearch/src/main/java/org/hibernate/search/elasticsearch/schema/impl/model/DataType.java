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
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-types.html
 * @author Yoann Rodiere
 */
public enum DataType {

	@SerializedName("object")
	OBJECT {
		@Override
		public boolean isComposite() {
			return true;
		}
	},
	/**
	 * @deprecated Only used in Elasticsearch 2.x. Use TEXT or KEYWORD instead.
	 */
	@SerializedName("string")
	@Deprecated
	STRING,
	@SerializedName("text")
	TEXT,
	@SerializedName("keyword")
	KEYWORD,
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

	public boolean isComposite() {
		return false;
	}
}
