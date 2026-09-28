package org.hibernate.search.backend.elasticsearch.client.common.spi;

/**
 * An interface allowing to close an {@link ElasticsearchClient}.
 */
public interface ElasticsearchClientImplementor extends ElasticsearchClient, AutoCloseable {

	@Override
	void close();

}
