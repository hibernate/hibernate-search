package org.hibernate.search.backend.elasticsearch.client.common.authentication.spi;

import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.util.common.annotation.Incubating;

/** Configuration-time provider role for clients supporting asynchronous request preparation. */
@Incubating
public interface ElasticsearchRequestPreparationProvider {
	/**
	 * Return empty when disabled, otherwise a shareable preparation and its owned resources.
	 * The integration retains the holder until client shutdown, after active attempts and their blocking actions have stopped. Cancelling an HTTP result
	 * does not establish that a blocked credentials lookup has returned.
	 */
	Optional<BeanHolder<ElasticsearchRequestPreparation>> provide(ElasticsearchRequestPreparationProviderContext context);
}
