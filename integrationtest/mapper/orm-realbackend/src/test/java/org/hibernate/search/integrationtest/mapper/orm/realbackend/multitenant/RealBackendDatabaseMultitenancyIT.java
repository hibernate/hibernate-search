/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.multitenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.common.impl.CollectionHelper.asSet;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.TenantId;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.AssertionAndAssumptionViolationFallThrough;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class RealBackendDatabaseMultitenancyIT {

	public static final String TENANT_ID_1 = "TENANT 1";
	public static final String TENANT_ID_2 = "TENANT 2";
	public static final String TENANT_ID_3 = "TENANT 3";
	public static final String TENANT_TEXT_1 = "I'm in the TENANT 1";
	public static final String TENANT_TEXT_2 = "I'm in the TENANT 2";

	@RegisterExtension
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private SessionFactory sessionFactory;

	@Test
	void multiTenancyStrategy_discriminator() throws InterruptedException {
		sessionFactory = setupHelper.start()
				.withProperty( "hibernate.search.backend.multi_tenancy.strategy", "discriminator" )
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy", "sync" )
				.tenants( TENANT_ID_1, TENANT_ID_2, TENANT_ID_3 )
				.setup( IndexedEntity.class );

		checkMultitenancy( (session, text) -> {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 1;
			entity.text = text;
			session.persist( entity );
		}, IndexedEntity.class );
	}

	@Test
	void multiTenancyStrategy_enabledByMapping() throws InterruptedException {
		sessionFactory = setupHelper.start()
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy", "sync" )
				.tenants( TENANT_ID_1, TENANT_ID_2, TENANT_ID_3 )
				.setup( IndexedEntity.class );

		checkMultitenancy( (session, text) -> {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 1;
			entity.text = text;
			session.persist( entity );
		}, IndexedEntity.class );
	}

	@TestForIssue(jiraKey = "HSEARCH-5031")
	@Test
	void multiTenancy_ormDiscriminator() throws InterruptedException {
		sessionFactory = setupHelper.start()
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy", "sync" )
				.tenants( false, TENANT_ID_1, TENANT_ID_2, TENANT_ID_3 )
				.withProperty( "hibernate.tenant_identifier_resolver", TenantIdentifierResolver.class )
				.setup( IndexedEntityWithTenant.class );

		AtomicInteger idGenerator = new AtomicInteger( 1 );
		checkMultitenancy( (session, text) -> {
			IndexedEntityWithTenant entity = new IndexedEntityWithTenant();
			entity.id = idGenerator.getAndIncrement();
			entity.text = text;
			session.persist( entity );
		}, IndexedEntityWithTenant.class );
	}

	@Test
	void multiTenancyStrategy_none() {
		assertThatThrownBy( () -> setupHelper.start()
				.withProperty( "hibernate.search.backend.multi_tenancy.strategy", "none" )
				.tenants( TENANT_ID_1, TENANT_ID_2 )
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

	private <T> void checkMultitenancy(BiConsumer<Session, String> creator, Class<T> type) throws InterruptedException {
		with( sessionFactory, TENANT_ID_1 ).runInTransaction( session -> {
			creator.accept( session, TENANT_TEXT_1 );
		} );
		with( sessionFactory, TENANT_ID_2 ).runInTransaction( session -> {
			creator.accept( session, TENANT_TEXT_2 );
		} );

		with( sessionFactory, TENANT_ID_1 ).runInTransaction( session -> {
			assertForCurrentTenant( session, type, TENANT_TEXT_1 );
		} );
		with( sessionFactory, TENANT_ID_2 ).runInTransaction( session -> {
			assertForCurrentTenant( session, type, TENANT_TEXT_2 );
		} );

		// and let's check mass indexing as well:
		SearchMapping searchMapping = Search.mapping( sessionFactory );
		searchMapping.scope( Object.class ).massIndexer( asSet( TENANT_ID_1, TENANT_ID_2, TENANT_ID_3 ) )
				// aws-serverless does not support purge, so we'll just drop the entire index here:
				.dropAndCreateSchemaOnStart( true )
				.startAndWait();

		with( sessionFactory, TENANT_ID_1 ).runInTransaction( session -> setupHelper.assertions()
				.searchAfterIndexChangesAndPotentialRefresh( () -> assertForCurrentTenant( session, type, TENANT_TEXT_1 ) ) );
		with( sessionFactory, TENANT_ID_2 ).runInTransaction( session -> setupHelper.assertions()
				.searchAfterIndexChangesAndPotentialRefresh( () -> assertForCurrentTenant( session, type, TENANT_TEXT_2 ) ) );
		with( sessionFactory, TENANT_ID_3 ).runInTransaction( session -> setupHelper.assertions()
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

	@Entity(name = IndexedEntityWithTenant.NAME)
	@Indexed(index = IndexedEntityWithTenant.NAME)
	public static final class IndexedEntityWithTenant {
		static final String NAME = "indexed-with-tenant";

		@Id
		private Integer id;
		@GenericField
		private String text;

		@TenantId
		private String tenantName;

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

	public static class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {
		private static final String ROOT = "root";
		public static InheritableThreadLocal<String> currentTenant = new InheritableThreadLocal<>();

		@Override
		public String resolveCurrentTenantIdentifier() {
			return currentTenant.get();
		}

		@Override
		public boolean validateExistingCurrentSessions() {
			return true;
		}

		@Override
		public boolean isRoot(String tenantID) {
			return ROOT.equals( currentTenant.get() );
		}
	}
}
