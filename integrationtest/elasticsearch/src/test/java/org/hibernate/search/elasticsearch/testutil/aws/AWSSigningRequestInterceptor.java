/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.testutil.aws;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.RequestLine;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.protocol.HttpContext;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.StringHelper;

import uk.co.lucasweb.aws.v4.signer.Signer;
import uk.co.lucasweb.aws.v4.signer.Signer.Builder;
import uk.co.lucasweb.aws.v4.signer.credentials.AwsCredentials;

/**
 * @author Yoann Rodiere
 */
public class AWSSigningRequestInterceptor implements HttpRequestInterceptor {

	private static final DateTimeFormatter AMZ_DATE_FORMATTER = new DateTimeFormatterBuilder()
			.appendValue( ChronoField.YEAR, 4 )
			.appendValue( ChronoField.MONTH_OF_YEAR, 2 )
			.appendValue( ChronoField.DAY_OF_MONTH, 2 )
			.appendLiteral( 'T' )
			.appendValue( ChronoField.HOUR_OF_DAY, 2 )
			.appendValue( ChronoField.MINUTE_OF_HOUR, 2 )
			.appendValue( ChronoField.SECOND_OF_MINUTE, 2 )
			.appendLiteral( 'Z' )
			.toFormatter();

	private static final String HOST_PORT_REGEX = ":\\d+$";

	private static final Comparator<? super String> QUERY_PARAMETER_NAME_COMPARATOR;
	static {
		Collator collator = Collator.getInstance( Locale.ROOT );
		collator.setStrength( Collator.SECONDARY );
		QUERY_PARAMETER_NAME_COMPARATOR = collator;
	}

	private final String accessKey;
	private final String secretKey;
	private final String region;
	private final String service;

	public AWSSigningRequestInterceptor(String accessKey, String secretKey, String region, String service) {
		super();
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.region = region;
		this.service = service;
	}

	@Override
	public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
		LocalDateTime now = LocalDateTime.now( ZoneOffset.UTC );
		String contentHash = getContentHash( request );
		sign( request, now, contentHash );
	}

	private void sign(HttpRequest request, LocalDateTime now, String contentHash) throws IOException {
		uk.co.lucasweb.aws.v4.signer.HttpRequest signerRequestLine = getSignerRequestLine( request );

		Signer.Builder builder = Signer.builder()
				.awsCredentials( new AwsCredentials( accessKey, secretKey ) )
				.region( region );
		Header hostHeader = request.getLastHeader( "host" );
		builder = builder.header( hostHeader.getName(), normalizeHost( hostHeader.getValue() ) );
		builder = addHeader( builder, request, "x-amz-date", AMZ_DATE_FORMATTER.format( now ) );
		builder = addHeader( builder, request, "x-amz-content-sha256", contentHash );

		Signer signer = builder.build( signerRequestLine, service, contentHash );

		request.addHeader( "Authorization", signer.getSignature() );
	}

	private uk.co.lucasweb.aws.v4.signer.HttpRequest getSignerRequestLine(HttpRequest request) {
		RequestLine requestLine = request.getRequestLine();
		URI uri = URI.create( requestLine.getUri() );
		return new FixedHttpRequest( requestLine.getMethod(), uri );
	}

	private Signer.Builder addHeader(Builder builder, HttpRequest request, String name, String value) {
		request.addHeader( name, value );
		return builder.header( name, value );
	}

	private String getContentHash(HttpRequest request) throws IOException {
		HttpEntity entity = getEntity( request );
		if ( entity == null ) {
			return DigestUtils.sha256Hex( "" );
		}
		if ( !entity.isRepeatable() ) {
			throw new IllegalStateException( "Cannot sign AWS requests with non-repeatable entities" );
		}
		try ( InputStream content = entity.getContent() ) {
			return DigestUtils.sha256Hex( content );
		}
	}

	private HttpEntity getEntity(HttpRequest request) throws IOException {
		if ( request instanceof HttpEntityEnclosingRequest ) {
			return ( (HttpEntityEnclosingRequest) request ).getEntity();
		}
		else {
			return null;
		}
	}

	private static String normalizeHost(String value) {
		return value.replaceAll( HOST_PORT_REGEX, "" );
	}

	private static String getNormalizedPath(URI uri) {
		// Use the raw path, i.e. the one we send to AWS
		String rawPath = uri.getRawPath();
		if ( StringHelper.isEmpty( rawPath ) ) {
			return "/";
		}
		else {
			/*
			 * For some unknown reason, AWS seems to URL-encode the path components
			 * before calculating the hash,
			 * even though the path components were already URL-encoded...
			 */
			StringBuilder builder = new StringBuilder();

			int componentStart = 0;
			int nextSeparator = rawPath.indexOf( '/' );
			while ( nextSeparator >= 0 ) {
				String pathComponent = rawPath.substring( componentStart, nextSeparator );
				builder.append( urlEncode( pathComponent ) ).append( '/' );
				componentStart = nextSeparator + 1;
				nextSeparator = rawPath.indexOf( '/', componentStart );
			}
			String pathComponent = rawPath.substring( componentStart );
			builder.append( urlEncode( pathComponent ) );

			return builder.toString();
		}
	}

	private static String urlEncode(String value) {
		try {
			return URLEncoder.encode( value, StandardCharsets.UTF_8.name() );
		}
		catch (UnsupportedEncodingException e) {
			throw new AssertionFailure( "Platform does not support UTF-8... ?", e );
		}
	}

	private static String getNormalizedQueryString(URI uri) {
		// Use the raw query, i.e. the one we send to AWS
		String rawQuery = uri.getRawQuery();
		if ( StringHelper.isEmpty( rawQuery ) ) {
			return "";
		}
		else {
			// Query parameters must be sorted alphabetically before being hashed
			List<NameValuePair> parameters = URLEncodedUtils.parse( uri, StandardCharsets.UTF_8.name() );
			parameters.sort( (l, r) -> QUERY_PARAMETER_NAME_COMPARATOR.compare( l.getName(), r.getName() ) );
			return URLEncodedUtils.format( parameters, StandardCharsets.UTF_8 );
		}
	}

	private static class FixedHttpRequest extends uk.co.lucasweb.aws.v4.signer.HttpRequest {

		private final URI uri;

		public FixedHttpRequest(String method, URI uri) {
			super( method, uri );
			this.uri = uri;
		}

		@Override
		public String getPath() {
			return getNormalizedPath( uri );
		}

		@Override
		public String getQuery() {
			return getNormalizedQueryString( uri );
		}
	}

}
