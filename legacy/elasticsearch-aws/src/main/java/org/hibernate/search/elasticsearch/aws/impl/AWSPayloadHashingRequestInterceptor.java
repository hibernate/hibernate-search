/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.aws.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import static org.apache.commons.codec.digest.DigestUtils.getSha256Digest;

/**
 * Interceptor computing the hash of the request payload.
 * <p>
 * Implemented separately from {@link AWSSigningRequestInterceptor} in order
 * to trigger content-length computation before the Apache HTTP client
 * generates the content-length header.
 *
 * @see <a href="https://hibernate.atlassian.net/browse/HSEARCH-2831">HSEARCH-2831</a>
 *
 * @author Yoann Rodiere
 */
class AWSPayloadHashingRequestInterceptor implements HttpRequestInterceptor {

	public static final String CONTEXT_ATTRIBUTE_HASH = AWSPayloadHashingRequestInterceptor.class.getName() + "_hash";

	private static final OutputStream DISCARDING_STREAM = new OutputStream() {
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
		}
		@Override
		public void write(int b) throws IOException {
		}
	};

	@Override
	public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
		String contentHash = computeContentHash( request );
		context.setAttribute( CONTEXT_ATTRIBUTE_HASH, contentHash );
	}

	private String computeContentHash(HttpRequest request) throws IOException {
		HttpEntity entity = getEntity( request );
		if ( entity == null ) {
			return DigestUtils.sha256Hex( "" );
		}
		if ( !entity.isRepeatable() ) {
			throw new IllegalStateException( "Cannot sign AWS requests with non-repeatable entities" );
		}

		final MessageDigest digest = getSha256Digest();
		DigestOutputStream digestStream = new DigestOutputStream( DISCARDING_STREAM, digest );
		entity.writeTo( digestStream );
		return Hex.encodeHexString( digest.digest() );
	}

	private HttpEntity getEntity(HttpRequest request) throws IOException {
		if ( request instanceof HttpEntityEnclosingRequest ) {
			return ( (HttpEntityEnclosingRequest) request ).getEntity();
		}
		else {
			return null;
		}
	}


}
