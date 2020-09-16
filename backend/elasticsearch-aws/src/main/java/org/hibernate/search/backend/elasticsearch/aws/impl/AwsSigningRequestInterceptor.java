/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.aws.impl;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.RequestLine;
import org.apache.http.protocol.HttpContext;
import uk.co.lucasweb.aws.v4.signer.Signer;
import uk.co.lucasweb.aws.v4.signer.credentials.AwsCredentials;

class AwsSigningRequestInterceptor implements HttpRequestInterceptor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final Set<String> HEADERS_TO_SIGN = CollectionHelper.asImmutableSet(
			AwsHeaders.HOST,
			AwsHeaders.X_AMZ_DATE_HEADER_NAME,
			AwsHeaders.X_AMZ_CONTENT_SHA256_HEADER_NAME
	);

	private final AwsCredentials credentials;
	private final String region;
	private final String service;

	AwsSigningRequestInterceptor(String accessKey, String secretKey, String region, String service) {
		this.credentials = new AwsCredentials( accessKey, secretKey );
		this.region = region;
		this.service = service;
	}

	@Override
	public void process(HttpRequest request, HttpContext context) {
		if ( log.isTraceEnabled() ) {
			log.tracef( "HTTP request (before signing): %s", request );
		}

		LocalDateTime now = LocalDateTime.now( ZoneOffset.UTC );
		request.addHeader( AwsHeaders.X_AMZ_DATE_HEADER_NAME, AwsHeaders.toAmzDate( now ) );

		String contentHash = (String) context.getAttribute( AwsPayloadHashingRequestInterceptor.CONTEXT_ATTRIBUTE_HASH );
		request.addHeader( AwsHeaders.X_AMZ_CONTENT_SHA256_HEADER_NAME, contentHash );

		request.addHeader( AwsHeaders.AUTHORIZATION, sign( request, contentHash ) );

		if ( log.isTraceEnabled() ) {
			log.tracef( "HTTP request (after signing): %s", request );
		}
	}

	private String sign(HttpRequest request, String contentHash) {
		RequestLine requestLine = request.getRequestLine();
		uk.co.lucasweb.aws.v4.signer.HttpRequest signerRequestLine =
				new uk.co.lucasweb.aws.v4.signer.HttpRequest( requestLine.getMethod(), requestLine.getUri() );

		Signer.Builder builder = Signer.builder()
				.awsCredentials( credentials )
				.region( region );

		for ( String headerName : HEADERS_TO_SIGN ) {
			Stream<String> stream = Arrays.stream( request.getHeaders( headerName ) )
					.map( Header::getValue );

			// Unspecified behavior: AWS does some extra normalization on the "host" header
			if ( AwsHeaders.HOST.equalsIgnoreCase( headerName ) ) {
				stream = stream.map( AwsNormalization::normalizeHost );
			}

			stream.forEach( v -> builder.header( headerName, v ) );
		}

		Signer signer = builder.build( signerRequestLine, service, contentHash );

		return signer.getSignature();
	}

}
