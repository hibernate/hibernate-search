package org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

/**
 * The search syntax for ES7.0 to ES8.0.
 */
public class Elasticsearch7SearchSyntax extends Elasticsearch81SearchSyntax {

	@Override
	public JsonNode encodeLongForAggregation(Long value) {
		// Workaround for https://github.com/elastic/elasticsearch/issues/81529
		return value == null ? NullNode.instance : JsonScalarConversions.toNode( value.toString() );
	}
}
