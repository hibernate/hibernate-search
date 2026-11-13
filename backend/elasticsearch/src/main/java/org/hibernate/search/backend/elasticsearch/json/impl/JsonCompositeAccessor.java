package org.hibernate.search.backend.elasticsearch.json.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;


/**
 * A {@link JsonAccessor} that can be crawled by a {@link AbstractCrawlingJsonAccessor}.
 *
 */
interface JsonCompositeAccessor<T extends JsonNode> extends JsonAccessor<T> {

	T getOrCreate(ObjectNode root);

}
