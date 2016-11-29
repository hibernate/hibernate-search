/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl.model;

import com.google.gson.annotations.SerializedName;

/**
 * An enum for Elasticsearch "dynamic" attribute values.
 *
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/dynamic.html
 * @author Yoann Rodiere
 */
public enum DynamicType {

	@SerializedName("true")
	TRUE,
	@SerializedName("false")
	FALSE,
	@SerializedName("strict")
	STRICT
	;
}
