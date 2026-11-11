package org.hibernate.search.backend.elasticsearch.aws.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.backend.elasticsearch.aws.spi.ElasticsearchAwsCredentialsProvider;
import org.hibernate.search.backend.elasticsearch.aws.spi.ElasticsearchAwsRequestSigning;
import org.hibernate.search.backend.elasticsearch.client.common.authentication.spi.ElasticsearchRequestPreparation;
import org.hibernate.search.backend.elasticsearch.client.common.authentication.spi.ElasticsearchRequestPreparationProviderContext;
import org.hibernate.search.backend.elasticsearch.client.common.authentication.spi.ElasticsearchRequestPreparations;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptorProvider;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequestInterceptorProviderContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.impl.MapConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.impl.BeanResolverImpl;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.ServiceResolver;
import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.spi.JsonBody;
import org.hibernate.search.util.json.spi.JsonWriteCursor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

class AsyncRequestSigningTest {
	private static final AwsCredentials CREDENTIALS = AwsBasicCredentials.create( "test-key", "test-secret" );
	private static final Clock CLOCK = Clock.fixed( Instant.parse( "2026-10-16T12:00:00Z" ), ZoneOffset.UTC );

	@ParameterizedTest
	@ValueSource(strings = { "es_node1", "es_node1:9200", "[::1]:9200" })
	void signsTheOriginalAuthority(String authority) {
		var signing = new ElasticsearchAwsRequestSigning( () -> CREDENTIALS, "us-east-1", "es", CLOCK );
		var request = new ElasticsearchRequestPreparation.Request( URI.create( "http://" + authority + "/_bulk" ), null );
		var result = signing.prepare( request, new Execution() ).toCompletableFuture().join();
		assertThat( header( result, "Host" ) ).isEqualTo( authority );
		assertThat( header( result, "Authorization" ) ).contains( "/es/aws4_request" );
	}

	@ParameterizedTest
	@ValueSource(strings = { "es", "aoss" })
	void signsReplayWithCallerExecutionAndClosesIndependentBoundedStreams(String service) throws Exception {
		var execution = new Execution();
		var body = new TrackedBody();
		var request = new ElasticsearchRequestPreparation.Request( URI.create( "https://host:9443/a%2Fb?q=x%20y" ),
				"POST", Map.of( "Content-Type", List.of( "application/json" ), "X-Test", List.of( "one", "two" ),
						"Content-Length", List.of( "123" ), "x-amz-security-token", List.of( "expired-token" ),
						"Authorization", List.of( "expired-signature" ) ),
				body );
		var signing = new ElasticsearchAwsRequestSigning( () -> CREDENTIALS, "us-east-1", service, CLOCK );
		for ( int attempt = 0; attempt < 2; attempt++ ) {
			var result = signing.prepare( request, execution ).toCompletableFuture().join();
			assertThat( header( result, "Authorization" ) ).contains( "/" + service + "/aws4_request" )
					.doesNotContain( "content-length" );
			assertThat( header( result, "x-amz-content-sha256" ) ).isEqualTo( body.sha256() );
			assertThat( result.headers() ).containsEntry( "Content-Length", List.of( "123" ) )
					.containsEntry( "X-Test", List.of( "one", "two" ) );
			assertThat( result.headers().keySet() ).noneMatch( name -> name.equalsIgnoreCase( "x-amz-security-token" ) );
		}
		assertThat( execution.calls ).hasValue( 2 );
		assertThat( body.opens ).hasValue( 2 );
		assertThat( body.closes ).hasValue( 2 );
		assertThat( body.maximumWrite ).hasValueLessThanOrEqualTo( JsonBodies.DEFAULT_BUFFER_SIZE );
		assertThat( request.headers().get( "Authorization" ) ).containsExactly( "expired-signature" );
	}

	@Test
	void absentBodySignsAnEmptyPayloadWithoutOpeningACursor() throws Exception {
		var signing = new ElasticsearchAwsRequestSigning( () -> CREDENTIALS, "us-east-1", "es", CLOCK );
		var result = signing.prepare( new ElasticsearchRequestPreparation.Request( URI.create( "https://host/" ),
				"GET", Map.of(), null ), new Execution() ).toCompletableFuture().join();
		assertThat( header( result, "x-amz-content-sha256" ) ).isEqualTo(
				HexFormat.of().formatHex( MessageDigest.getInstance( "SHA-256" ).digest( new byte[0] ) ) );
	}

	@Test
	void cancellationDuringHashingClosesTheSigningCursor() {
		var execution = new Execution();
		var body = new TrackedBody();
		body.afterWrite = () -> execution.cancelled.set( true );
		var signing = new ElasticsearchAwsRequestSigning( () -> CREDENTIALS, "us-east-1", "es", CLOCK );
		assertThatThrownBy( () -> signing.prepare( new ElasticsearchRequestPreparation.Request(
				URI.create( "https://host/" ), body ), execution ).toCompletableFuture().join() )
				.satisfies( failure -> assertThat( rootFailure( failure ) ).isInstanceOf( CancellationException.class ) );
		assertThat( body.opens ).hasValue( 1 );
		assertThat( body.closes ).hasValue( 1 );
	}

	@Test
	void cancelledCredentialLookupFailureAndRejectedExecutionNeverOpenBody() {
		var body = new TrackedBody();
		var request = new ElasticsearchRequestPreparation.Request( URI.create( "https://host/" ), body );
		var execution = new Execution();
		var signing = new ElasticsearchAwsRequestSigning( () -> {
			execution.cancelled.set( true );
			return CREDENTIALS;
		}, "us-east-1", "es", CLOCK );
		assertThatThrownBy( () -> signing.prepare( request, execution ).toCompletableFuture().join() )
				.satisfies( failure -> assertThat( rootFailure( failure ) ).isInstanceOf( CancellationException.class ) );
		var failingCredentials = new ElasticsearchAwsRequestSigning( () -> {
			throw new IllegalStateException( "credentials unavailable" );
		}, "us-east-1", "es", CLOCK );
		assertThatThrownBy( () -> failingCredentials.prepare( request, new Execution() ).toCompletableFuture().join() )
				.hasCauseInstanceOf( IllegalStateException.class );
		var rejected = new Execution() {
			@Override
			public <T> CompletionStage<T> blocking(Callable<T> action) {
				throw new RejectedExecutionException( "integration queue full" );
			}
		};
		assertThatThrownBy( () -> signing.prepare( request, rejected ).toCompletableFuture().join() )
				.hasCauseInstanceOf( RejectedExecutionException.class );
		assertThat( body.opens ).hasValue( 0 );
	}

	@Test
	void disabledConfigurationDoesNotResolveCredentialsForEitherRole() {
		var resolver = mock( BeanResolver.class );
		var context = new ProviderContext( resolver, Map.of() );
		var provider = new ElasticsearchAwsSigningInterceptorProvider();
		assertThat( provider.provide( (ElasticsearchRequestPreparationProviderContext) context ) ).isEmpty();
		assertThat( provider.provide( (ElasticsearchRequestInterceptorProviderContext) context ) ).isEmpty();
		verifyNoInteractions( resolver );
	}

	@ParameterizedTest
	@ValueSource(strings = { "opensearch", "amazon-opensearch-serverless" })
	@SuppressWarnings({ "unchecked", "unused" })
	void configurationSelectsServiceAndOwnsCreatedCredentialsUntilShutdown(String distribution) {
		var resolver = mock( BeanResolver.class );
		var credentials = new CloseableCredentials();
		ElasticsearchAwsCredentialsProvider factory = properties -> credentials;
		doReturn( BeanHolder.of( factory ) ).when( resolver ).resolve( any( BeanReference.class ) );
		var context = new ProviderContext( resolver,
				Map.of( "aws.signing.enabled", true, "aws.region", "us-east-1", "version", distribution ) );
		var provider = new ElasticsearchAwsSigningInterceptorProvider();
		try ( var holder = provider.provide( (ElasticsearchRequestPreparationProviderContext) context ).orElseThrow() ) {
			assertThat( credentials.closes ).hasValue( 0 );
			var signed =
					holder.get().prepare( new ElasticsearchRequestPreparation.Request( URI.create( "https://host/" ), null ),
							new Execution() ).toCompletableFuture().join();
			assertThat( header( signed, "Authorization" ) ).contains(
					"/" + ( "amazon-opensearch-serverless".equals( distribution ) ? "aoss" : "es" ) + "/aws4_request" );
		}
		assertThat( credentials.closes ).hasValue( 1 );
	}

	@Test
	void configuredAwsRoleIsDiscoveredThroughTheProductionBeanResolver() {
		var services = mock( ServiceResolver.class );
		when( services.loadJavaServices( BeanConfigurer.class ) ).thenReturn( List.of( new ElasticsearchAwsBeanConfigurer() ) );
		var resolver = BeanResolverImpl.create( mock( ClassResolver.class ), services, null,
				new MapConfigurationPropertySource( Map.of() ) );
		var properties = new MapConfigurationPropertySource( Map.of(
				"aws.signing.enabled", true, "aws.region", "us-east-1", "aws.credentials.type", "static",
				"aws.credentials.access_key_id", "configured-key", "aws.credentials.secret_access_key", "configured-secret" ) );
		try ( var preparation = ElasticsearchRequestPreparations.create( resolver, properties ) ) {
			var result = preparation.get()
					.prepare( new ElasticsearchRequestPreparation.Request( URI.create( "https://host/" ), null ),
							new Execution() )
					.toCompletableFuture().join();
			assertThat( header( result, "Authorization" ) ).contains( "Credential=configured-key/" );
		}
		assertThat( resolver.allConfiguredForRole( ElasticsearchRequestInterceptorProvider.class ) ).hasSize( 1 );
	}

	@Test
	void bothProviderRolesRetainRequiredRegionValidation() {
		var context = new ProviderContext( mock( BeanResolver.class ), Map.of( "aws.signing.enabled", true ) );
		var provider = new ElasticsearchAwsSigningInterceptorProvider();
		assertThatThrownBy( () -> provider.provide( (ElasticsearchRequestPreparationProviderContext) context ) )
				.hasMessageContaining( "aws.region" );
		assertThatThrownBy( () -> provider.provide( (ElasticsearchRequestInterceptorProviderContext) context ) )
				.hasMessageContaining( "aws.region" );
	}

	private static Throwable rootFailure(Throwable failure) {
		while ( failure.getCause() != null ) {
			failure = failure.getCause();
		}
		return failure;
	}

	private static String header(ElasticsearchRequestPreparation.Result result, String name) {
		return result.headers().entrySet().stream().filter( entry -> name.equalsIgnoreCase( entry.getKey() ) )
				.findFirst().orElseThrow().getValue().get( 0 );
	}

	private static class Execution implements ElasticsearchRequestPreparation.Execution {
		private final AtomicBoolean cancelled = new AtomicBoolean();
		private final AtomicInteger calls = new AtomicInteger();

		public <T> CompletionStage<T> blocking(Callable<T> action) {
			calls.incrementAndGet();
			try {
				return CompletableFuture.completedFuture( action.call() );
			}
			catch (Exception failure) {
				return CompletableFuture.failedFuture( failure );
			}
		}

		public void checkActive() {
			if ( cancelled.get() ) {
				throw new CancellationException( "cancelled" );
			}
		}
	}

	private static final class TrackedBody implements JsonBody {
		private final JsonBody delegate =
				JsonBodies.of( new JsonParsing().parse( "{\"large\":\"" + "é😀".repeat( 10000 ) + "\"}" ) );
		private final AtomicInteger opens = new AtomicInteger();
		private final AtomicInteger closes = new AtomicInteger();
		private final AtomicInteger maximumWrite = new AtomicInteger();
		private Runnable afterWrite = () -> {};

		public JsonWriteCursor openCursor() {
			opens.incrementAndGet();
			var delegateCursor = delegate.openCursor();
			Thread owner = Thread.currentThread();
			return new JsonWriteCursor() {
				public int write(ByteBuffer destination) {
					assertThat( Thread.currentThread() ).isSameAs( owner );
					int written = delegateCursor.write( destination );
					maximumWrite.accumulateAndGet( written, Math::max );
					afterWrite.run();
					return written;
				}

				public boolean isComplete() {
					return delegateCursor.isComplete();
				}

				public void close() {
					assertThat( Thread.currentThread() ).isSameAs( owner );
					closes.incrementAndGet();
					delegateCursor.close();
				}
			};
		}

		private String sha256() throws Exception {
			try ( var stream = delegate.openStream() ) {
				return HexFormat.of().formatHex( MessageDigest.getInstance( "SHA-256" ).digest( stream.readAllBytes() ) );
			}
		}
	}

	private record ProviderContext(BeanResolver beanResolver, Map<String, ?> properties)
			implements ElasticsearchRequestPreparationProviderContext, ElasticsearchRequestInterceptorProviderContext {
		public ConfigurationPropertySource configurationPropertySource() {
			return new MapConfigurationPropertySource( properties );
		}
	}

	private static final class CloseableCredentials implements AwsCredentialsProvider, AutoCloseable {
		private final AtomicInteger closes = new AtomicInteger();

		public AwsCredentials resolveCredentials() {
			return CREDENTIALS;
		}

		public void close() {
			closes.incrementAndGet();
		}
	}
}
