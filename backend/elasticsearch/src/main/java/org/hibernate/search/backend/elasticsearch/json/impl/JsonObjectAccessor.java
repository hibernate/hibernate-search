package org.hibernate.search.backend.elasticsearch.json.impl;

import tools.jackson.databind.node.ObjectNode;


public interface JsonObjectAccessor extends JsonAccessor<ObjectNode> {

	UnknownTypeJsonAccessor property(String propertyName);

}
