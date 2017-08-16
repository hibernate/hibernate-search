/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.aws.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.RequestLine;
import org.apache.http.protocol.HttpContext;
import org.hibernate.search.util.impl.CollectionHelper;

import uk.co.lucasweb.aws.v4.signer.Signer;
import uk.co.lucasweb.aws.v4.signer.credentials.AwsCredentials;

/**
 * @author Yoann Rodiere
 */
class AWSSigningRequestInterceptor implements HttpRequestInterceptor {

	private static final Set<String> HEADERS_TO_SIGN = CollectionHelper.asImmutableSet(new String[] {
			AWSHeaders.HOST,
			AWSHeaders.X_AMZ_DATE_HEADER_NAME,
			AWSHeaders.X_AMZ_CONTENT_SHA256_HEADER_NAME
	});

	private final AwsCredentials credentials;
	private final String region;
	private final String service;

	public AWSSigningRequestInterceptor(String accessKey, String secretKey, String region, String service) {
		this.credentials = new AwsCredentials( accessKey, secretKey );
		this.region = region;
		this.service = service;
	}

	@Override
	public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
		LocalDateTime now = LocalDateTime.now( ZoneOffset.UTC );
		request.addHeader( AWSHeaders.X_AMZ_DATE_HEADER_NAME, AWSHeaders.toAmzDate( now ) );

		String contentHash = (String) context.getAttribute( AWSPayloadHashingRequestInterceptor.CONTEXT_ATTRIBUTE_HASH );
		request.addHeader( AWSHeaders.X_AMZ_CONTENT_SHA256_HEADER_NAME, contentHash );

		request.addHeader( AWSHeaders.AUTHORIZATION, sign( request, contentHash ) );
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
			if ( AWSHeaders.HOST.equalsIgnoreCase( headerName ) ) {
				stream = stream.map( AWSNormalization::normalizeHost );
			}

			stream.forEach( v -> builder.header( headerName, v ) );
		}

		Signer signer = builder.build( signerRequestLine, service, contentHash );

		return signer.getSignature();
	}

}
