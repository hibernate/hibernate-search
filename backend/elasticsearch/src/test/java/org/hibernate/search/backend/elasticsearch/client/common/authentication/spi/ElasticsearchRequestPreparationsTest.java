package org.hibernate.search.backend.elasticsearch.client.common.authentication.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.engine.cfg.impl.MapConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.internal.spi.JsonParsing;

import org.junit.jupiter.api.Test;

class ElasticsearchRequestPreparationsTest {
	private static final ElasticsearchRequestPreparation.Execution INLINE = new ElasticsearchRequestPreparation.Execution() {
		@Override
		public <T> CompletionStage<T> blocking(Callable<T> action) {
			throw new AssertionFailure( "Composition must not choose a blocking execution policy" );
		}

		@Override
		public void checkActive() {
		}
	};

	@Test
	void snapshotAndResultCopyHeaderValuesButRetainTheReplayableBody() {
		var values = new ArrayList<>( List.of( "one", "two" ) );
		var headers = new LinkedHashMap<String, List<String>>();
		headers.put( "X-Test", values );
		var body = JsonBodies.of( new JsonParsing().parse( "{}" ) );
		var request = new ElasticsearchRequestPreparation.Request( URI.create( "https://host/a%2Fb?q=a%20b" ),
				"PUT", headers, body );
		var result = new ElasticsearchRequestPreparation.Result( headers );
		values.clear();
		headers.clear();
		assertThat( request.headers().get( "X-Test" ) ).containsExactly( "one", "two" );
		assertThat( result.headers() ).isEqualTo( request.headers() );
		assertThat( request.body() ).isSameAs( body );
		assertThat( request.uri().getRawPath() ).isEqualTo( "/a%2Fb" );
		assertThatThrownBy( () -> result.headers().clear() ).isInstanceOf( UnsupportedOperationException.class );
		assertThatThrownBy( () -> request.headers().get( "X-Test" ).clear() )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	void rejectsUrisThatDoNotDescribeAnEffectiveHttpDestination() {
		for ( String uri : List.of( "/relative", "ftp://host/", "https://user@host/", "https://host/#fragment",
				"http://user@es_node1:9200/", "http://es_node1:bad/", "http://:9200/", "http:opaque",
				"http://es_node1/#fragment", "http://es%40node/", "http://-es_node1/" ) ) {
			assertThatThrownBy( () -> new ElasticsearchRequestPreparation.Request( URI.create( uri ), null ) )
					.isInstanceOf( IllegalArgumentException.class );
		}
	}

	@Test
	void preservesEffectiveHostAndPortIncludingUnderscoresAndIpv6() {
		for ( String host : List.of( "host", "es_node1", "_es_node_", "[::1]" ) ) {
			for ( String port : List.of( "", ":9200" ) ) {
				URI uri = URI.create( "http://" + host + port + "/a%2Fb?q=a%20b" );
				var request = new ElasticsearchRequestPreparation.Request( uri, null );
				assertThat( request.uri() ).isEqualTo( uri );
				assertThat( request.host() ).isEqualTo( host );
				assertThat( request.port() ).isEqualTo( port.isEmpty() ? -1 : 9200 );
			}
		}
	}

	@Test
	void compositionWaitsForEachProviderAndPropagatesHeadersWithoutChangingDestination() {
		var request = new ElasticsearchRequestPreparation.Request( URI.create( "https://host/" ), null );
		var gate = new CompletableFuture<ElasticsearchRequestPreparation.Result>();
		var calls = new AtomicInteger();
		ElasticsearchRequestPreparation first = (snapshot, execution) -> gate;
		ElasticsearchRequestPreparation second = (snapshot, execution) -> {
			assertThat( snapshot.uri() ).isEqualTo( request.uri() );
			assertThat( snapshot.body() ).isSameAs( request.body() );
			assertThat( snapshot.headers() ).containsEntry( "X-First", List.of( "first" ) );
			calls.incrementAndGet();
			return CompletableFuture
					.completedFuture( new ElasticsearchRequestPreparation.Result( Map.of( "X-Last", List.of( "last" ) ) ) );
		};
		var result = ElasticsearchRequestPreparations.compose( List.of( first, second ) ).prepare( request, INLINE );
		assertThat( calls ).hasValue( 0 );
		gate.complete( new ElasticsearchRequestPreparation.Result( Map.of( "X-First", List.of( "first" ) ) ) );
		assertThat( result.toCompletableFuture().join().headers() ).containsOnlyKeys( "X-Last" );
		assertThat( calls ).hasValue( 1 );
	}

	@Test
	void synchronousProviderFailureAndCancellationPreventLaterPreparation() {
		var request = new ElasticsearchRequestPreparation.Request( URI.create( "https://host/" ), null );
		ElasticsearchRequestPreparation fail = (snapshot, execution) -> {
			throw new IllegalStateException( "authentication failed" );
		};
		ElasticsearchRequestPreparation unexpected = (snapshot, execution) -> {
			throw new AssertionFailure( "Must not prepare after failure" );
		};
		assertThatThrownBy( () -> ElasticsearchRequestPreparations.compose( List.of( fail, unexpected ) )
				.prepare( request, INLINE ).toCompletableFuture().join() ).hasCauseInstanceOf( IllegalStateException.class );
		var cancelled = new ElasticsearchRequestPreparation.Execution() {
			public <T> CompletionStage<T> blocking(Callable<T> action) {
				throw new AssertionFailure( "Unexpected blocking submission" );
			}

			public void checkActive() {
				throw new CancellationException( "cancelled" );
			}
		};
		assertThatThrownBy( () -> ElasticsearchRequestPreparations.compose( List.of( unexpected ) )
				.prepare( request, cancelled ).toCompletableFuture().join() ).hasCauseInstanceOf( CancellationException.class );
	}

	@Test
	void configuredProvidersAndTheirResourcesRemainOwnedUntilClientShutdown() {
		BeanResolver resolver = mock( BeanResolver.class );
		var reference = BeanReference.of( ElasticsearchRequestPreparationProvider.class, "test" );
		var references = List.of( reference );
		when( resolver.allConfiguredForRole( ElasticsearchRequestPreparationProvider.class ) ).thenReturn( references );
		var source = new MapConfigurationPropertySource( Map.of() );
		AtomicInteger providerCloses = new AtomicInteger();
		AtomicInteger preparationCloses = new AtomicInteger();
		ElasticsearchRequestPreparationProvider provider = context -> {
			assertThat( context.beanResolver() ).isSameAs( resolver );
			assertThat( context.configurationPropertySource() ).isSameAs( source );
			return Optional.of( holder( ElasticsearchRequestPreparations.compose( List.of() ), preparationCloses ) );
		};
		when( resolver.resolve( references ) ).thenReturn( holder( List.of( provider ), providerCloses ) );
		try ( var owned = ElasticsearchRequestPreparations.create( resolver, source ) ) {
			assertThat( providerCloses ).hasValue( 0 );
			assertThat( preparationCloses ).hasValue( 0 );
			assertThat( owned.get() ).isNotNull();
		}
		assertThat( providerCloses ).hasValue( 1 );
		assertThat( preparationCloses ).hasValue( 1 );
	}

	@Test
	void setupFailureClosesAlreadyCreatedPreparationsAndProviderBeans() {
		BeanResolver resolver = mock( BeanResolver.class );
		var reference = BeanReference.of( ElasticsearchRequestPreparationProvider.class, "test" );
		var references = List.of( reference );
		when( resolver.allConfiguredForRole( ElasticsearchRequestPreparationProvider.class ) ).thenReturn( references );
		var closes = new AtomicInteger();
		ElasticsearchRequestPreparationProvider first = context -> Optional.of(
				holder( ElasticsearchRequestPreparations.compose( List.of() ), closes ) );
		ElasticsearchRequestPreparationProvider failure = context -> {
			throw new IllegalStateException( "setup failed" );
		};
		when( resolver.resolve( references ) ).thenReturn( holder( List.of( first, failure ), closes ) );
		assertThatThrownBy( () -> ElasticsearchRequestPreparations.create( resolver,
				new MapConfigurationPropertySource( Map.of() ) ) ).isInstanceOf( IllegalStateException.class );
		assertThat( closes ).hasValue( 2 );
	}

	private static <T> BeanHolder<T> holder(T value, AtomicInteger closes) {
		return new BeanHolder<>() {
			public T get() {
				return value;
			}

			public void close() {
				closes.incrementAndGet();
			}
		};
	}
}
