package org.hibernate.search.backend.elasticsearch.client.common.authentication.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.impl.SuppressingCloser;

/** Discovery and ordered composition, without creating an executor or touching transport objects. */
@Incubating
public final class ElasticsearchRequestPreparations {
	private ElasticsearchRequestPreparations() {
	}

	/** Retain this holder until client shutdown; close after all active attempts and blocking actions have stopped. */
	public static BeanHolder<ElasticsearchRequestPreparation> create(BeanResolver beanResolver,
			ConfigurationPropertySource propertySource) {
		var providers =
				beanResolver.resolve( beanResolver.allConfiguredForRole( ElasticsearchRequestPreparationProvider.class ) );
		List<BeanHolder<ElasticsearchRequestPreparation>> preparations = new ArrayList<>();
		try {
			var context = new ElasticsearchRequestPreparationProviderContext() {
				@Override
				public BeanResolver beanResolver() {
					return beanResolver;
				}

				@Override
				public ConfigurationPropertySource configurationPropertySource() {
					return propertySource;
				}
			};
			for ( var provider : providers.get() ) {
				provider.provide( context ).ifPresent( preparations::add );
			}
			BeanHolder<List<ElasticsearchRequestPreparation>> owned = BeanHolder.of( preparations );
			return BeanHolder.of( compose( owned.get() ) ).withDependencyAutoClosing( owned, providers );
		}
		catch (RuntimeException | Error failure) {
			new SuppressingCloser( failure ).pushAll( preparations ).push( providers );
			throw failure;
		}
	}

	/**
	 * Execute in list order. Each provider sees the previous complete headers and the same body/destination.
	 * Header-mutating preparations must precede the final signer; no dependency order is inferred.
	 * This helper does not own the supplied preparations; synchronous failures also become failed stages.
	 */
	public static ElasticsearchRequestPreparation compose(List<? extends ElasticsearchRequestPreparation> preparations) {
		var ordered = List.copyOf( preparations );
		return (request, execution) -> {
			CompletionStage<ElasticsearchRequestPreparation.Result> result = CompletableFuture.completedFuture(
					new ElasticsearchRequestPreparation.Result( request.headers() ) );
			for ( var preparation : ordered ) {
				result = result.thenCompose( previous -> {
					execution.checkActive();
					return Objects
							.requireNonNull( preparation.prepare( request.withHeaders( previous.headers() ), execution ) );
				} );
			}
			return result.thenApply( prepared -> {
				execution.checkActive();
				return Objects.requireNonNull( prepared );
			} );
		};
	}
}
