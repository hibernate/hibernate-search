package org.hibernate.search.backend.elasticsearch.client.common.authentication.spi;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.annotation.Incubating;

/** Backend configuration, already masked to its root; the key "type" remains reserved for the engine. */
@Incubating
public interface ElasticsearchRequestPreparationProviderContext {
	BeanResolver beanResolver();

	ConfigurationPropertySource configurationPropertySource();
}
