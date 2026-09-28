package org.hibernate.search.backend.elasticsearch.client.common.util.spi;


public final class ElasticsearchClientUtils {

	private ElasticsearchClientUtils() {
		// Private constructor
	}

	public static boolean isSuccessCode(int code) {
		return 200 <= code && code < 300;
	}

}
