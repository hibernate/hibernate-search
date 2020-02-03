/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public interface ElasticsearchSearchSyntax {

	String getTermAggregationOrderByTermToken();

	boolean useOldSortNestedApi();

	void requestDocValues(JsonObject requestBody, JsonPrimitive fieldName);

}
