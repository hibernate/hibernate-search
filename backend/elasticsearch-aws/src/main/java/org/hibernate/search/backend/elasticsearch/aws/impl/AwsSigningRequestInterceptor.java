/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.aws.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.aws.logging.impl.AwsLog;
import org.hibernate.search.util.common.AssertionFailure;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.RequestLine;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.regions.Region;

class AwsSigningRequestInterceptor implements HttpRequestInterceptor {

	private final AwsV4HttpSigner signer;
	private final Region region;
	private final String service;
	private final AwsCredentialsProvider credentialsProvider;

	AwsSigningRequestInterceptor(Region region, String service, AwsCredentialsProvider credentialsProvider) {
		this.signer = AwsV4HttpSigner.create();
		this.region = region;
		this.service = service;
		this.credentialsProvider = credentialsProvider;
	}

	@Override
	public void process(HttpRequest request, HttpContext context) throws IOException {
		try ( HttpEntityContentStreamProvider contentStreamProvider = extractEntityContent( request ) ) {
			sign( request, context, contentStreamProvider );
		}
	}

	private void sign(HttpRequest request, HttpContext context, HttpEntityContentStreamProvider contentStreamProvider) {
		SdkHttpFullRequest awsRequest = toAwsRequest( request, context, contentStreamProvider );

		if ( AwsLog.INSTANCE.isTraceEnabled() ) {
			AwsLog.INSTANCE.httpRequestBeforeSigning( request );
			AwsLog.INSTANCE.awsRequestBeforeSigning( awsRequest );
		}

		AwsCredentials credentials = credentialsProvider.resolveCredentials();
		AwsLog.INSTANCE.awsCredentials( credentials );

		SignedRequest signedRequest = signer.sign( r -> r.identity( credentials )
				.request( awsRequest )
				.payload( awsRequest.contentStreamProvider().orElse( null ) )
				.putProperty( AwsV4HttpSigner.SERVICE_SIGNING_NAME, service )
				.putProperty( AwsV4HttpSigner.REGION_NAME, region.id() ) );

		// The AWS SDK added some headers.
		// Let's just override the existing headers with whatever the AWS SDK came up with.
		// We don't expect signing to affect anything else (path, query, content, ...).
		for ( Map.Entry<String, List<String>> header : signedRequest.request().headers().entrySet() ) {
			String name = header.getKey();
			boolean first = true;
			for ( String value : header.getValue() ) {
				if ( first ) {
					request.setHeader( name, value );
					first = false;
				}
				else {
					request.addHeader( name, value );
				}
			}
		}

		if ( AwsLog.INSTANCE.isTraceEnabled() ) {
			AwsLog.INSTANCE.httpRequestAfterSigning( signedRequest );
			AwsLog.INSTANCE.awsRequestAfterSigning( request );
		}
	}

	private SdkHttpFullRequest toAwsRequest(HttpRequest request, HttpContext context,
			ContentStreamProvider contentStreamProvider) {
		SdkHttpFullRequest.Builder awsRequestBuilder = SdkHttpFullRequest.builder();

		HttpCoreContext coreContext = HttpCoreContext.adapt( context );
		HttpHost targetHost = coreContext.getTargetHost();
		awsRequestBuilder.host( targetHost.getHostName() );
		awsRequestBuilder.port( targetHost.getPort() );
		awsRequestBuilder.protocol( targetHost.getSchemeName() );

		RequestLine requestLine = request.getRequestLine();
		awsRequestBuilder.method( SdkHttpMethod.fromValue( requestLine.getMethod() ) );

		String pathAndQuery = requestLine.getUri();
		String path;
		List<NameValuePair> queryParameters;
		int queryStart = pathAndQuery.indexOf( '?' );
		if ( queryStart >= 0 ) {
			path = pathAndQuery.substring( 0, queryStart );
			queryParameters = URLEncodedUtils.parse( pathAndQuery.substring( queryStart + 1 ), StandardCharsets.UTF_8 );
		}
		else {
			path = pathAndQuery;
			queryParameters = Collections.emptyList();
		}

		// For some reason this is needed on Amazon OpenSearch Serverless
		if ( "aoss".equals( service ) ) {
			awsRequestBuilder.appendHeader( "x-amz-content-sha256", "required" );
		}

		awsRequestBuilder.encodedPath( path );
		for ( NameValuePair param : queryParameters ) {
			awsRequestBuilder.appendRawQueryParameter( param.getName(), param.getValue() );
		}

		// Do NOT copy the headers, as the AWS SDK will sometimes sign some headers
		// that are not properly taken into account by the AWS servers (e.g. content-length).

		awsRequestBuilder.contentStreamProvider( contentStreamProvider );

		return awsRequestBuilder.build();
	}

	private HttpEntityContentStreamProvider extractEntityContent(HttpRequest request) {
		if ( request instanceof HttpEntityEnclosingRequest ) {
			HttpEntity entity = ( (HttpEntityEnclosingRequest) request ).getEntity();
			if ( entity == null ) {
				return null;
			}
			if ( !entity.isRepeatable() ) {
				throw new AssertionFailure( "Cannot sign AWS requests with non-repeatable entities" );
			}
			return new HttpEntityContentStreamProvider( entity );
		}
		else {
			return null;
		}
	}

}
