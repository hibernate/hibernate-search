/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
