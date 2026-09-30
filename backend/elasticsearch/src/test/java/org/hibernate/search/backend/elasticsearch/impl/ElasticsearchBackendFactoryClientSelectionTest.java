package org.hibernate.search.backend.elasticsearch.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.impl.ClientJdkElasticsearchClientFactory;
import org.hibernate.search.engine.cfg.impl.MapConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class ElasticsearchBackendFactoryClientSelectionTest {

	@Mock
	private BeanResolver beanResolver;

	@Test
	void jdkWhenNoOtherClientIsRegistered() {
		BeanReference<ElasticsearchClientFactory> jdk = reference( "jdk" );
		register( jdk, List.of( jdk ) );

		assertThat( select( Map.of() ).toString() ).contains( ClientJdkElasticsearchClientFactory.NAME );
	}

	@Test
	void selectOnlyOtherClient() {
		BeanReference<ElasticsearchClientFactory> jdk = reference( "jdk" );
		BeanReference<ElasticsearchClientFactory> rest4 = reference( "rest4" );
		register( jdk, List.of( jdk, rest4 ) );

		assertThat( select( Map.of() ) ).isSameAs( rest4 );
	}

	@Test
	void failWhenMultipleOtherClientsAreRegistered() {
		BeanReference<ElasticsearchClientFactory> jdk = reference( "jdk" );
		BeanReference<ElasticsearchClientFactory> rest4 = reference( "rest4" );
		BeanReference<ElasticsearchClientFactory> rest5 = reference( "rest5" );
		register( jdk, List.of( jdk, rest4, rest5 ) );

		assertThatThrownBy( () -> select( Map.of() ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "client_factory" );
	}

	@Test
	void explicitSelectionWinsWhenMultipleOtherClientsAreRegistered() {
		assertThat( select( Map.of( ElasticsearchBackendSettings.CLIENT_FACTORY, "elasticsearch-rest4" ) ).toString() )
				.contains( "elasticsearch-rest4" );
	}

	private void register(BeanReference<ElasticsearchClientFactory> jdk,
			List<BeanReference<ElasticsearchClientFactory>> all) {
		when( beanResolver.namedConfiguredForRole( ElasticsearchClientFactory.class ) )
				.thenReturn( Map.of( ClientJdkElasticsearchClientFactory.NAME, jdk ) );
		when( beanResolver.allConfiguredForRole( ElasticsearchClientFactory.class ) ).thenReturn( all );
	}

	private BeanReference<? extends ElasticsearchClientFactory> select(Map<String, ?> properties) {
		return ElasticsearchBackendFactory.selectClientFactoryReference(
				new MapConfigurationPropertySource( properties ), beanResolver );
	}

	private static BeanReference<ElasticsearchClientFactory> reference(String name) {
		return BeanReference.of( ElasticsearchClientFactory.class, name );
	}
}
