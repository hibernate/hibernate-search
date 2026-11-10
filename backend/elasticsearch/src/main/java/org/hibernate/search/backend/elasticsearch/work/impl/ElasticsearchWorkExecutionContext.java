package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonClient;
import org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi.JsonProvider;

public interface ElasticsearchWorkExecutionContext {

	ElasticsearchJsonClient getClient();

	JsonProvider getJsonProvider();

}
