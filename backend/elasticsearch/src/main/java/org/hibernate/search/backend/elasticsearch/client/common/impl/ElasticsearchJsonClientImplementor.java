package org.hibernate.search.backend.elasticsearch.client.common.impl;

/**
 * An interface allowing to close an {@link ElasticsearchJsonClient}.
 */
public interface ElasticsearchJsonClientImplementor extends ElasticsearchJsonClient, AutoCloseable {

	@Override
	void close();

}
