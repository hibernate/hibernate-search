/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;

public interface ElasticsearchWorkExecutionContext {

	ElasticsearchClient getClient();

	GsonProvider getGsonProvider();

}
