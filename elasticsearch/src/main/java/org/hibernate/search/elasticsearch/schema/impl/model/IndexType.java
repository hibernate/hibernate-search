/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl.model;

import com.google.gson.annotations.SerializedName;

/**
 * An enum for Elasticsearch "index" attribute values.
 *
 * @see https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-index.html
 * @author Yoann Rodiere
 */
public enum IndexType {

	@SerializedName("not_analyzed")
	NOT_ANALYZED,
	@SerializedName("analyzed")
	ANALYZED,
	@SerializedName("no")
	NO
	;
}
