package org.hibernate.search.backend.elasticsearch.client.common.spi;

/** Transport lifecycle, owned by Search. */
public interface ElasticsearchClientImplementor extends ElasticsearchClient, AutoCloseable {
	@Override
	void close();
}
