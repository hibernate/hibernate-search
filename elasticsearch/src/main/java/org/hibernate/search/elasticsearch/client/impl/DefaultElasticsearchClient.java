/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.sniff.Sniffer;
import org.hibernate.search.elasticsearch.dialect.impl.DialectIndependentGsonProvider;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.logging.impl.ElasticsearchLogCategories;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchClientUtils;
import org.hibernate.search.util.impl.Closer;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchClient implements ElasticsearchClientImplementor {

	private static final Log log = LoggerFactory.make( Log.class );

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
	public ElasticsearchResponse execute(ElasticsearchRequest request) throws IOException {
		Response response = doExecute( request );

		try {
			JsonObject body = parseBody( response );
			return new ElasticsearchResponse(
					response.getStatusLine().getStatusCode(),
					response.getStatusLine().getReasonPhrase(),
					body );
		}
		catch (IOException | RuntimeException e) {
			throw log.failedToParseElasticsearchResponse(
					response.getStatusLine().getStatusCode(),
					response.getStatusLine().getReasonPhrase(),
					e );
		}
	}

	private Response doExecute(ElasticsearchRequest request) throws IOException {
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

	private JsonObject parseBody(Response response) throws IOException {
		HttpEntity entity = response.getEntity();
		if ( entity == null ) {
			return null;
		}

		Gson gson = gsonProvider.getGson();
		Charset charset = getCharset( entity );
		try ( InputStream inputStream = entity.getContent();
				Reader reader = new InputStreamReader( inputStream, charset ) ) {
			return gson.fromJson( reader, JsonObject.class );
		}
	}

	private static Charset getCharset(HttpEntity entity) {
		ContentType contentType = ContentType.get( entity );
		Charset charset = contentType.getCharset();
		return charset != null ? charset : StandardCharsets.UTF_8;
	}

	@Override
	public void close() throws IOException {
		try ( Closer<IOException> closer = new Closer<>() ) {
			if ( this.sniffer != null ) {
				closer.push( this.sniffer::close );
			}
			closer.push( this.restClient::close );
		}
	}

}
