/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl;

import com.google.gson.JsonElement;
import com.google.gson.LongSerializationPolicy;

/**
 * The search syntax for ES7.0 to ES8.0.
 */
public class Elasticsearch7SearchSyntax extends Elasticsearch81SearchSyntax {

	@Override
	public JsonElement encodeLongForAggregation(Long value) {
		// Workaround for https://github.com/elastic/elasticsearch/issues/81529
		return LongSerializationPolicy.STRING.serialize( value );
	}
}
