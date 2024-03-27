/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.multitenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.common.impl.CollectionHelper.asSet;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.TenantId;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.tenancy.TenantIdentifierConverter;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.AssertionAndAssumptionViolationFallThrough;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedPerClass;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ParameterizedPerClass
class RealBackendDatabaseMultitenancyIT {

	public static final String TENANT_TEXT_1 = "I'm in the TENANT 1";
	public static final String TENANT_TEXT_2 = "I'm in the TENANT 2";

	@RegisterExtension
	public static OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private Object tenant1;
	private Object tenant2;
	private Object tenant3;
	private TenantIdentifierConverter converter;
	private Class<? extends AbstractIndexedEntityWithTenant> entityWithTenant;

	public static List<? extends Arguments> params() {
		return List.of(
				Arguments.of( "TENANT 1", "TENANT 2", "TENANT 3", null, IndexedEntityWithStringTenant.class ),
				Arguments.of( 1, 2, 3, new TenantIdentifierConverter() {
					@Override
					public String toStringValue(Object tenantId) {
						return Objects.toString( tenantId, null );
					}

					@Override
					public Object fromStringValue(String tenantId) {
						return tenantId == null ? null : Integer.parseInt( tenantId );
					}
				}, IndexedEntityWithIntegerTenant.class ),
				Arguments.of(
						UUID.fromString( "55555555-7777-6666-9999-000000000001" ),
						UUID.fromString( "55555555-7777-6666-9999-000000000002" ),
						UUID.fromString( "55555555-7777-6666-9999-000000000003" ),
						new TenantIdentifierConverter() {
							@Override
							public String toStringValue(Object tenantId) {
								return Objects.toString( tenantId, null );
							}

							@Override
							public Object fromStringValue(String tenantId) {
								return tenantId == null ? null : UUID.fromString( tenantId );
							}
						},
						IndexedEntityWithUUIDTenant.class
				)
		);
	}

	@ParameterizedSetup
	@MethodSource("params")
	void setup(Object tenant1, Object tenant2, Object tenant3, TenantIdentifierConverter converter,
			Class<? extends AbstractIndexedEntityWithTenant> entityWithTenant) {
		this.tenant1 = tenant1;
		this.tenant2 = tenant2;
		this.tenant3 = tenant3;
		this.converter = converter;
		this.entityWithTenant = entityWithTenant;
	}

	private OrmSetupHelper.SetupContext preconfigureConverter() {
		OrmSetupHelper.SetupContext setupContext = setupHelper.start();
		if ( converter != null ) {
			setupContext.withProperty(
					HibernateOrmMapperSettings.MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER,
					BeanReference.ofInstance( converter )
			);
		}
		return setupContext;
	}

	@Test
	void multiTenancyStrategy_discriminator() throws InterruptedException {
		SessionFactory sessionFactory = preconfigureConverter()
				.withProperty( "hibernate.search.backend.multi_tenancy.strategy", "discriminator" )
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy", "sync" )
				.tenantsWithHelperEnabled( tenant1, tenant2, tenant3 )
				.setup( IndexedEntity.class );

		checkMultitenancy( sessionFactory, (session, text) -> {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 1;
			entity.text = text;
			session.persist( entity );
		}, IndexedEntity.class );
	}

	@Test
	void multiTenancyStrategy_enabledByMapping() throws InterruptedException {
		SessionFactory sessionFactory = preconfigureConverter()
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy", "sync" )
				.tenantsWithHelperEnabled( tenant1, tenant2, tenant3 )
				.setup( IndexedEntity.class );

		checkMultitenancy( sessionFactory, (session, text) -> {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 1;
			entity.text = text;
			session.persist( entity );
		}, IndexedEntity.class );
	}

	@TestForIssue(jiraKey = "HSEARCH-5031")
	@Test
	void multiTenancy_ormDiscriminator() throws InterruptedException {
		SessionFactory sessionFactory = preconfigureConverter()
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy", "sync" )
				.tenants( false, tenant1, tenant2, tenant3 )
				.withProperty( "hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class )
				.setup( entityWithTenant );

		AtomicInteger idGenerator = new AtomicInteger( 1 );
		checkMultitenancy( sessionFactory, (session, text) -> {
			AbstractIndexedEntityWithTenant entity = null;
			try {
				entity = entityWithTenant.getDeclaredConstructor().newInstance();
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
			entity.id = idGenerator.getAndIncrement();
			entity.text = text;
			session.persist( entity );
		}, entityWithTenant );
	}

	@Test
	void multiTenancyStrategy_none() {
		assertThatThrownBy( () -> preconfigureConverter()
				.withProperty( "hibernate.search.backend.multi_tenancy.strategy", "none" )
				.tenantsWithHelperEnabled( tenant1, tenant2 )
				.setup( IndexedEntity.class ) )
				// This is necessary to correctly rethrow assumption failures (when not using H2)
				.satisfies( AssertionAndAssumptionViolationFallThrough.get() )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.defaultBackendContext()
						.failure( "Invalid backend configuration: " +
								"mapping requires multi-tenancy but no multi-tenancy strategy is set" )
				);
	}

	private <T> void checkMultitenancy(SessionFactory sessionFactory, BiConsumer<Session, String> creator, Class<T> type)
			throws InterruptedException {
		with( sessionFactory, tenant1 ).runInTransaction( session -> {
			creator.accept( session, TENANT_TEXT_1 );
		} );
		with( sessionFactory, tenant2 ).runInTransaction( session -> {
			creator.accept( session, TENANT_TEXT_2 );
		} );

		with( sessionFactory, tenant1 ).runInTransaction( session -> {
			assertForCurrentTenant( session, type, TENANT_TEXT_1 );
		} );
		with( sessionFactory, tenant2 ).runInTransaction( session -> {
			assertForCurrentTenant( session, type, TENANT_TEXT_2 );
		} );

		// and let's check mass indexing as well:
		SearchMapping searchMapping = Search.mapping( sessionFactory );
		searchMapping.scope( Object.class ).massIndexer( asSet( tenant1, tenant2, tenant3 ) )
				// aws-serverless does not support purge, so we'll just drop the entire index here:
				.dropAndCreateSchemaOnStart( true )
				.startAndWait();

		with( sessionFactory, tenant1 ).runInTransaction( session -> setupHelper.assertions()
				.searchAfterIndexChangesAndPotentialRefresh( () -> assertForCurrentTenant( session, type, TENANT_TEXT_1 ) ) );
		with( sessionFactory, tenant2 ).runInTransaction( session -> setupHelper.assertions()
				.searchAfterIndexChangesAndPotentialRefresh( () -> assertForCurrentTenant( session, type, TENANT_TEXT_2 ) ) );
		with( sessionFactory, tenant3 ).runInTransaction( session -> setupHelper.assertions()
				.searchAfterIndexChangesAndPotentialRefresh( () -> assertThat( Search.session( session )
						.search( type )
						.where( SearchPredicateFactory::matchAll )
						.fetchTotalHitCount() )
						.isZero() ) );
	}

	private static void assertForCurrentTenant(Session session, Class<?> type, String text) {
		List<?> entities = Search.session( session )
				.search( type )
				.where( SearchPredicateFactory::matchAll )
				.fetchAllHits();
		assertThat( entities ).extracting( "text" ).containsExactly( text );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.NAME)
	public static final class IndexedEntity {
		static final String NAME = "indexed";

		@Id
		private Integer id;
		@GenericField
		private String text;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@Entity
	public static final class IndexedEntityWithStringTenant extends AbstractIndexedEntityWithTenant {

		@TenantId
		private String tenantName;
	}

	@Entity
	public static final class IndexedEntityWithIntegerTenant extends AbstractIndexedEntityWithTenant {
		@TenantId
		private Integer tenantName;
	}

	@Entity
	public static final class IndexedEntityWithUUIDTenant extends AbstractIndexedEntityWithTenant {
		@TenantId
		private UUID tenantName;
	}

	@MappedSuperclass
	@Indexed(index = IndexedEntityWithUUIDTenant.NAME)
	public abstract static class AbstractIndexedEntityWithTenant {
		static final String NAME = "indexed-with-tenant";

		@Id
		Integer id;
		@GenericField
		String text;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	public static class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Object> {
		private static final String ROOT = "root";
		public static InheritableThreadLocal<Object> currentTenant = new InheritableThreadLocal<>();

		@Override
		public Object resolveCurrentTenantIdentifier() {
			return currentTenant.get();
		}

		@Override
		public boolean validateExistingCurrentSessions() {
			return true;
		}

		@Override
		public boolean isRoot(Object tenantID) {
			return ROOT.equals( currentTenant.get() );
		}
	}
}
