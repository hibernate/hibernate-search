/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.aws.impl;

import java.io.IOException;

import org.hibernate.search.backend.elasticsearch.aws.logging.impl.AwsLog;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptor;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptorContext;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.regions.Region;

class ElasticsearchAwsSigningRequestInterceptor implements ElasticsearchRequestInterceptor {

	private final AwsV4HttpSigner signer;
	private final Region region;
	private final String service;
	private final AwsCredentialsProvider credentialsProvider;

	ElasticsearchAwsSigningRequestInterceptor(Region region, String service, AwsCredentialsProvider credentialsProvider) {
		this.signer = AwsV4HttpSigner.create();
		this.region = region;
		this.service = service;
		this.credentialsProvider = credentialsProvider;
	}

	@Override
	public void intercept(ElasticsearchRequestInterceptorContext requestContext) throws IOException {
		try ( HttpEntityContentStreamProvider contentStreamProvider =
				HttpEntityContentStreamProvider.create( requestContext ) ) {
			sign( requestContext, contentStreamProvider );
		}
	}

	private void sign(ElasticsearchRequestInterceptorContext requestContext,
			HttpEntityContentStreamProvider contentStreamProvider) {
		SdkHttpFullRequest awsRequest = toAwsRequest( requestContext, contentStreamProvider );

		if ( AwsLog.INSTANCE.isTraceEnabled() ) {
			AwsLog.INSTANCE.httpRequestBeforeSigning( requestContext );
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
		requestContext.overrideHeaders( signedRequest.request().headers() );

		if ( AwsLog.INSTANCE.isTraceEnabled() ) {
			AwsLog.INSTANCE.httpRequestAfterSigning( signedRequest );
			AwsLog.INSTANCE.awsRequestAfterSigning( requestContext );
		}
	}

	private SdkHttpFullRequest toAwsRequest(
			ElasticsearchRequestInterceptorContext requestContext,
			ContentStreamProvider contentStreamProvider) {
		SdkHttpFullRequest.Builder awsRequestBuilder = SdkHttpFullRequest.builder();

		awsRequestBuilder.host( requestContext.host() );
		awsRequestBuilder.port( requestContext.port() );
		awsRequestBuilder.protocol( requestContext.scheme() );

		awsRequestBuilder.method( SdkHttpMethod.fromValue( requestContext.method() ) );

		String path = requestContext.path();

		// For some reason this is needed on Amazon OpenSearch Serverless
		if ( "aoss".equals( service ) ) {
			awsRequestBuilder.appendHeader( "x-amz-content-sha256", "required" );
		}

		awsRequestBuilder.encodedPath( path );
		for ( var param : requestContext.queryParameters().entrySet() ) {
			awsRequestBuilder.appendRawQueryParameter( param.getKey(), param.getValue() );
		}

		// Do NOT copy the headers, as the AWS SDK will sometimes sign some headers
		// that are not properly taken into account by the AWS servers (e.g. content-length).

		awsRequestBuilder.contentStreamProvider( contentStreamProvider );

		return awsRequestBuilder.build();
	}

}
