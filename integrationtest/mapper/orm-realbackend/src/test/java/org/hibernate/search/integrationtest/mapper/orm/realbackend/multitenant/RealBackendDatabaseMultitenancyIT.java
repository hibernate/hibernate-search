/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.multitenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.function.Consumer;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.AssertionAndAssumptionViolationFallThrough;

import org.junit.Rule;
import org.junit.Test;

public class RealBackendDatabaseMultitenancyIT {

	public static final String TENANT_ID_1 = "TENANT 1";
	public static final String TENANT_ID_2 = "TENANT 2";
	public static final String TENANT_TEXT_1 = "I'm in the TENANT 1";
	public static final String TENANT_TEXT_2 = "I'm in the TENANT 2";

	@Rule
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private SessionFactory sessionFactory;

	@Test
	public void multiTenancyStrategy_discriminator() {
		sessionFactory = setupHelper.start()
				.withProperty( "hibernate.search.backend.multi_tenancy.strategy", "discriminator" )
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy", "sync" )
				.tenants( TENANT_ID_1, TENANT_ID_2 )
				.setup( IndexedEntity.class );

		checkMultitenancy();
	}

	@Test
	public void multiTenancyStrategy_enabledByMapping() {
		sessionFactory = setupHelper.start()
				.withProperty( "hibernate.search.indexing.plan.synchronization.strategy", "sync" )
				.tenants( TENANT_ID_1, TENANT_ID_2 )
				.setup( IndexedEntity.class );

		checkMultitenancy();
	}

	@Test
	public void multiTenancyStrategy_none() {
		assertThatThrownBy( () ->
				setupHelper.start()
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

	private void checkMultitenancy() {
		withinTransaction( TENANT_ID_1, session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 1;
			entity.text = TENANT_TEXT_1;
			session.persist( entity );
		} );
		withinTransaction( TENANT_ID_2, session -> {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 1;
			entity.text = TENANT_TEXT_2;
			session.persist( entity );
		} );

		withinSession( TENANT_ID_1, session -> {
			List<IndexedEntity> entities = Search.session( session )
					.search( IndexedEntity.class )
					.where( f -> f.matchAll() )
					.fetchAllHits();

			assertThat( entities ).extracting( "text" ).containsExactly( TENANT_TEXT_1 );
		} );
		withinSession( TENANT_ID_2, session -> {
			List<IndexedEntity> entities = Search.session( session )
					.search( IndexedEntity.class )
					.where( f -> f.matchAll() )
					.fetchAllHits();

			assertThat( entities ).extracting( "text" ).containsExactly( TENANT_TEXT_2 );
		} );
	}

	private void withinTransaction(String tenantId, Consumer<Session> action) {
		withinSession( tenantId, (session) -> {
			session.beginTransaction();
			action.accept( session );
			session.getTransaction().commit();
			session.clear();
		} );
	}

	private void withinSession(String tenantId, Consumer<Session> action) {
		try ( Session session = sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession() ) {
			action.accept( session );
		}
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
}
