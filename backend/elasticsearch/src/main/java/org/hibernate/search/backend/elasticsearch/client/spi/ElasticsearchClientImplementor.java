/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.spi;

/**
 * An interface allowing to close an {@link ElasticsearchClient}.
 */
public interface ElasticsearchClientImplementor extends ElasticsearchClient, AutoCloseable {

	@Override
	void close();

}
