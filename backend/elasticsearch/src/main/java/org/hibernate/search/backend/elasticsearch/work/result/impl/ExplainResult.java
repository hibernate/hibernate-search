package org.hibernate.search.backend.elasticsearch.work.result.impl;

import tools.jackson.databind.node.ObjectNode;

public interface ExplainResult {

	ObjectNode getJsonObject();
}
