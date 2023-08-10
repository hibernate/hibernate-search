/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.bootstrap;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource.fromMap;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Collections;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.cfg.spi.ConfigurationProvider;
import org.hibernate.search.engine.cfg.spi.ConfigurationScopeNamespaces;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Rule;
import org.junit.Test;

public class ConfigurationProviderIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	public void smoke() {
		backendMock.expectAnySchema( INDEX_NAME );
		backendMock.onCreate( context -> {
			assertThat( context.backendConfigurationPropertySource().get( "some.backend.setting" ) )
					.isPresent()
					.get().isEqualTo( false );
		} );
		backendMock.onCreateIndex( context -> {
			assertThat( context.getPropertySource().get( "some.index.setting" ) )
					.isPresent()
					.get().isEqualTo( 123 );
		} );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( EngineSpiSettings.BEAN_CONFIGURERS, Collections.singletonList(
						(BeanConfigurer) context -> context.define(
								ConfigurationProvider.class,
								BeanReference.ofInstance( scope -> {
									if ( scope.matchExact( ConfigurationScopeNamespaces.GLOBAL ) ) {
										return Optional.of( fromMap( singletonMap(
												HibernateOrmMapperSettings.Radicals.INDEXING_LISTENERS_ENABLED,
												false
										) ) );
									}
									if ( scope.matchAny( ConfigurationScopeNamespaces.BACKEND ) ) {
										return Optional.of( fromMap( singletonMap(
												"some.backend.setting",
												false
										) ) );
									}
									if ( scope.matchAny( ConfigurationScopeNamespaces.INDEX ) ) {
										return Optional.of( fromMap( singletonMap(
												"some.index.setting",
												123
										) ) );
									}
									return Optional.empty();
								} ) )
				) )
				.setup( IndexedEntity.class );

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity = new IndexedEntity();
			session.persist( entity );
		} );

		// no work is expected since indexing listeners are disabled for a global scope
		backendMock.verifyExpectationsMet();

	}

	@Entity(name = "IndexedEntity")
	@Indexed(index = INDEX_NAME)
	private static class IndexedEntity {
		@Id
		private Integer id = 1;

		@FullTextField
		private String string = "string";
	}
}
