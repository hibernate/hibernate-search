package org.hibernate.search.backend.elasticsearch.aws.spi;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.backend.elasticsearch.client.common.authentication.spi.ElasticsearchRequestPreparation;
import org.hibernate.search.util.common.annotation.Incubating;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;

/**
 * AWS SigV4 request preparation using only the integration-supplied blocking execution facility.
 * No executor or credential provider is created here; callers own the supplied credentials and their lifecycle.
 * Signing streams are bounded, independent from transmission, and closed on their owning blocking thread.
 * Uses the synchronous SDK signer: its asynchronous payload-checksum path may buffer the complete payload.
 */
@Incubating
public final class ElasticsearchAwsRequestSigning implements ElasticsearchRequestPreparation {
	private static final Set<String> UNSIGNED_TRANSPORT_HEADERS = Set.of(
			"content-length", "transfer-encoding", "connection", "expect", "user-agent" );
	private static final Set<String> AUTHENTICATION_HEADERS = Set.of(
			"authorization", "x-amz-date", "x-amz-security-token", "x-amz-content-sha256" );
	private final AwsCredentialsProvider credentials;
	private final String region;
	private final String service;
	private final Clock clock;
	private final AwsV4HttpSigner signer = AwsV4HttpSigner.create();

	public ElasticsearchAwsRequestSigning(AwsCredentialsProvider credentials, String region, String service, Clock clock) {
		this.credentials = Objects.requireNonNull( credentials );
		this.region = Objects.requireNonNull( region );
		this.service = Objects.requireNonNull( service );
		this.clock = Objects.requireNonNull( clock );
	}

	@Override
	public CompletionStage<Result> prepare(Request attempt, Execution execution) {
		Objects.requireNonNull( attempt );
		Objects.requireNonNull( execution );
		try {
			return execution.blocking( () -> {
				execution.checkActive();
				var identity = credentials.resolveCredentials();
				execution.checkActive();
				Map<String, List<String>> headers = new LinkedHashMap<>();
				attempt.headers().forEach( (name, values) -> {
					if ( !AUTHENTICATION_HEADERS.contains( name.toLowerCase( Locale.ROOT ) ) ) {
						headers.put( name, values );
					}
				} );
				var request = SdkHttpRequest.builder().uri( attempt.uri() )
						.host( attempt.host() ).port( attempt.port() )
						.method( SdkHttpMethod.fromValue( attempt.method() ) );
				headers.forEach( (name, values) -> {
					if ( !UNSIGNED_TRANSPORT_HEADERS.contains( name.toLowerCase( Locale.ROOT ) ) ) {
						request.putHeader( name, values );
					}
				} );
				// Serverless requires the content checksum; match the existing synchronous integration.
				if ( "aoss".equals( service ) ) {
					request.putHeader( "x-amz-content-sha256", "required" );
				}
				try ( var streams = attempt.body() == null ? null : new SigningStreams( attempt, execution ) ) {
					var signed = signer.sign( r -> r.identity( identity ).request( request.build() ).payload( streams )
							.putProperty( AwsV4HttpSigner.REGION_NAME, region )
							.putProperty( AwsV4HttpSigner.SERVICE_SIGNING_NAME, service )
							.putProperty( HttpSigner.SIGNING_CLOCK, clock ) );
					execution.checkActive();
					signed.request().headers().forEach( (name, values) -> {
						headers.keySet().removeIf( old -> old.equalsIgnoreCase( name ) );
						headers.put( name, values );
					} );
					return new Result( headers );
				}
			} );
		}
		catch (RuntimeException failure) {
			return CompletableFuture.failedFuture( failure );
		}
	}

	/** Only the worker opens, reads and closes signing cursors. Cancellation sets a signal. */
	private static final class SigningStreams implements ContentStreamProvider, AutoCloseable {
		private final Request attempt;
		private final Execution execution;
		private InputStream previous;

		private SigningStreams(Request attempt, Execution execution) {
			this.attempt = attempt;
			this.execution = execution;
		}

		@Override
		public InputStream newStream() {
			close();
			execution.checkActive();
			previous = new FilterInputStream( attempt.body().openStream() ) {
				@Override
				public int read() throws IOException {
					execution.checkActive();
					return in.read();
				}

				@Override
				public int read(byte[] target, int offset, int length) throws IOException {
					execution.checkActive();
					return in.read( target, offset, length );
				}
			};
			return previous;
		}

		@Override
		public void close() {
			if ( previous != null ) {
				try {
					previous.close();
				}
				catch (IOException e) {
					throw new UncheckedIOException( e );
				}
				finally {
					previous = null;
				}
			}
		}
	}
}
