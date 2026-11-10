package org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;

/** Default bounded buffer and decoded response policies shared by HTTP transports. */
public final class JsonTransportSupport {
	public static final int DEFAULT_BUFFER_SIZE = 8192;
	public static final int DEFAULT_INPUT_CAPACITY = 8192;
	public static final long DEFAULT_LIMIT = 0;

	private static final ConfigurationProperty<Long> RESPONSE_BODY_SIZE_LIMIT =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.RESPONSE_BODY_SIZE_LIMIT )
					.asLongPositiveOrZeroOrNegative()
					.validate( limit -> {
						if ( limit < 0 ) {
							throw ElasticsearchClientLog.INSTANCE.negativeResponseBodySizeLimit( limit );
						}
					} )
					.withDefault( DEFAULT_LIMIT ).build();

	/** Read the decoded-byte limit from a backend-masked client configuration; zero means unlimited. */
	public static long responseBodySizeLimit(ConfigurationPropertySource propertySource) {
		return RESPONSE_BODY_SIZE_LIMIT.get( propertySource );
	}

	private JsonTransportSupport() {
	}
}
