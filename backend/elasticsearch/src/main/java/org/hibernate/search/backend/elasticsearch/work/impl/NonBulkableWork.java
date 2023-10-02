/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import java.util.concurrent.CompletableFuture;

public interface NonBulkableWork<T> extends ElasticsearchWork {

	CompletableFuture<T> execute(ElasticsearchWorkExecutionContext context);

}
