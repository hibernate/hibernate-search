/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.bootstrap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateOrmIntegrationBooter;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandleFactory;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.HibernateOrmMappingHandle;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Fail;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class HibernateOrmIntegrationBooterIT {

	private static final String INDEX_NAME = "IndexName";

	private final List<AutoCloseable> toClose = new ArrayList<>();

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock
	private ValueReadHandle<Integer> idValueReadHandleMock;

	@Mock
	private ValueReadHandle<String> textValueReadHandleMock;

	@Mock
	private ValueReadHandleFactory valueReadHandleFactoryMock;

	@After
	public void cleanup() throws Exception {
		try ( Closer<Exception> closer = new Closer<>() ) {
			closer.pushAll( AutoCloseable::close, toClose );
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void twoPhaseBoot() throws Exception {
		CompletableFuture<BackendMappingHandle> mappingHandlePromise = new CompletableFuture<>();
		HibernateOrmIntegrationBooter booter = createBooter( mappingHandlePromise, IndexedEntity.class );
		Map<String, Object> booterGeneratedProperties = new LinkedHashMap<>();

		// Pre-booting should lead to a schema definition in the backend.
		backendMock.expectSchema( INDEX_NAME, b -> b.field( "text", String.class,
				b2 -> b2.analyzerName( AnalyzerNames.DEFAULT ) ) );
		// Pre-booting should retrieve value-read handles
		// Simulate a custom handle from a framework, e.g. Quarkus
		when( valueReadHandleFactoryMock.createForField( IndexedEntity.ID_FIELD ) )
				.thenReturn( (ValueReadHandle) idValueReadHandleMock );
		when( valueReadHandleFactoryMock.createForField( IndexedEntity.TEXT_FIELD ) )
				.thenReturn( (ValueReadHandle) textValueReadHandleMock );
		booter.preBoot( booterGeneratedProperties::put );
		backendMock.verifyExpectationsMet();
		verify( valueReadHandleFactoryMock ).createForField( IndexedEntity.ID_FIELD );
		verify( valueReadHandleFactoryMock ).createForField( IndexedEntity.TEXT_FIELD );

		SimpleSessionFactoryBuilder builder = new SimpleSessionFactoryBuilder()
				.addAnnotatedClass( IndexedEntity.class )
				/*
				 * We use a "trapped" mapping configurer to check that Hibernate Search does not generate the mapping,
				 * but re-uses the one generated by the "pre-boot" above.
				 */
				.setProperty( HibernateOrmMapperSettings.MAPPING_CONFIGURER, new HibernateOrmSearchMappingConfigurer() {
					@Override
					public void configure(HibernateOrmMappingConfigurationContext context) {
						Fail.fail( "Hibernate Search did not re-use the mapping generated when pre-booting" );
					}
				} );

		for ( Map.Entry<String, Object> booterGeneratedProperty : booterGeneratedProperties.entrySet() ) {
			builder.setProperty( booterGeneratedProperty.getKey(), booterGeneratedProperty.getValue() );
		}

		// Actually booting the session factory should lead to a schema creation in the backend.
		backendMock.expectSchemaManagementWorks( INDEX_NAME )
				.work( StubSchemaManagementWork.Type.CREATE_OR_VALIDATE );
		try ( SessionFactory sessionFactory = builder.build() ) {
			mappingHandlePromise.complete( new HibernateOrmMappingHandle( sessionFactory ) );

			/*
			 * Building the session should NOT lead to a second schema creation in the backend:
			 * that would mean the pre-boot was ignored...
			 */
			backendMock.verifyExpectationsMet();

			OrmUtils.withinTransaction( sessionFactory, session -> {
				IndexedEntity entity = new IndexedEntity();
				entity.id = 1;
				entity.text = "someText";
				session.persist( entity );

				when( textValueReadHandleMock.get( entity ) ).thenReturn( entity.text );

				backendMock.expectWorks( INDEX_NAME )
						.add( "1", b -> b.field( "text", "someText" ) );
			} );
			// If the entity was indexed, it means Hibernate Search booted correctly
			backendMock.verifyExpectationsMet();

			// Also check that the value handle has been called
			verify( textValueReadHandleMock ).get( any() );
		}
	}

	private HibernateOrmIntegrationBooter createBooter(CompletableFuture<BackendMappingHandle> mappingHandlePromise,
			Class<?> ... entityClasses) {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();

		// Configure the backend
		registryBuilder.applySetting(
				EngineSettings.BACKEND + "." + BackendSettings.TYPE,
				backendMock.factory( mappingHandlePromise )
		);

		StandardServiceRegistry serviceRegistry = registryBuilder.build();
		toClose.add( serviceRegistry );

		MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		for ( Class<?> entityClass : entityClasses ) {
			metadataSources.addAnnotatedClass( entityClass );
		}
		Metadata metadata = metadataSources.buildMetadata();

		MetadataImplementor metadataImplementor = (MetadataImplementor) metadata;
		BootstrapContext bootstrapContext =
				metadataImplementor.getTypeConfiguration().getMetadataBuildingContext().getBootstrapContext();

		return HibernateOrmIntegrationBooter.builder( metadata, bootstrapContext )
				.valueReadHandleFactory( valueReadHandleFactoryMock )
				.build();
	}

	@Entity(name = INDEX_NAME)
	@Indexed(index = INDEX_NAME)
	private static class IndexedEntity {
		private static final Field ID_FIELD;
		private static final Field TEXT_FIELD;

		static {
			try {
				ID_FIELD = IndexedEntity.class.getDeclaredField( "id" );
				TEXT_FIELD = IndexedEntity.class.getDeclaredField( "text" );
			}
			catch (NoSuchFieldException e) {
				throw new IllegalStateException( e );
			}
		}

		@Id
		private Integer id;
		@FullTextField
		private String text;
	}
}