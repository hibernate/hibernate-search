/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.sniff.Sniffer;
import org.hibernate.search.elasticsearch.dialect.impl.DialectIndependentGsonProvider;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.logging.impl.ElasticsearchLogCategories;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchClientUtils;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.Gson;

/**
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchClient implements ElasticsearchClientImplementor {

	private static final Log requestLog = LoggerFactory.make( Log.class, ElasticsearchLogCategories.REQUEST );

	private final RestClient restClient;

	private final Sniffer sniffer;

	private volatile GsonProvider gsonProvider;

	public DefaultElasticsearchClient(RestClient restClient, Sniffer sniffer) {
		this.restClient = restClient;
		this.sniffer = sniffer;
		this.gsonProvider = DialectIndependentGsonProvider.INSTANCE;
	}

	@Override
	public void init(GsonProvider gsonProvider) {
		this.gsonProvider = gsonProvider;
	}

	@Override
	public Response execute(ElasticsearchRequest request) throws IOException {
		Gson gson = gsonProvider.getGson();
		HttpEntity entity = ElasticsearchClientUtils.toEntity( gson, request );
		long start = System.nanoTime();
		try {
			return restClient.performRequest(
					request.getMethod(),
					request.getPath(),
					request.getParameters(),
					entity
			);
		}
		catch (ResponseException e) {
			requestLog.debug( "ES client issued a ResponseException - not necessarily a problem", e );
			/*
			 * The client tries to guess what's an error and what's not, but it's too naive.
			 * A 404 on DELETE is not always important to us, for instance.
			 * Thus we ignore the exception and do our own checks afterwards.
			 */
			return e.getResponse();
		}
		finally {
			long executionTime = System.nanoTime() - start;
			requestLog.executedRequest( request.getPath(), request.getParameters(), TimeUnit.NANOSECONDS.toMillis( executionTime ) );
		}
	}

	@Override
	public void close() throws IOException {
		try ( RestClient restClient = this.restClient;
				Sniffer sniffer = this.sniffer; ) {
			/*
			 * Nothing to do: we simply take advantage of Java's auto-closing,
			 * which adds suppressed exceptions as needed and always tries
			 * to close every resource.
			 */
		}
	}

}
