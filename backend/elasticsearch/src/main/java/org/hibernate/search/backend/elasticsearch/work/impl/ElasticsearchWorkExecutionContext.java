package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.common.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClient;

public interface ElasticsearchWorkExecutionContext {

	ElasticsearchClient getClient();

	GsonProvider getGsonProvider();

}
