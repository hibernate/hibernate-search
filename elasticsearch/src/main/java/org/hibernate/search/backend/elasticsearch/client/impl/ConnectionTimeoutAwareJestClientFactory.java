/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;

import org.apache.http.client.config.RequestConfig;

/**
 * Temporary work-around for https://github.com/searchbox-io/Jest/issues/227
 *
 * @author Gunnar Morling
 */
final class ConnectionTimeoutAwareJestClientFactory extends JestClientFactory {

	private HttpClientConfig httpClientConfig;

	@Override
	public void setHttpClientConfig(HttpClientConfig httpClientConfig) {
		super.setHttpClientConfig( httpClientConfig );
		this.httpClientConfig = httpClientConfig;
	}

	@Override
	protected RequestConfig getRequestConfig() {
		return RequestConfig.custom()
				.setConnectTimeout( httpClientConfig.getConnTimeout() )
				.setSocketTimeout( httpClientConfig.getReadTimeout() )
				.build();
	}
}
