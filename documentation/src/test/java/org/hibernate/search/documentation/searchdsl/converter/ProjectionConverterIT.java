/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.searchdsl.converter;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.projection.ProjectionConverter;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmAutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ProjectionConverterIT {
	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	public ProjectionConverterIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						HibernateOrmAutomaticIndexingSynchronizationStrategyName.SEARCHABLE
				)
				.setup( Order.class );
		initData();
	}

	@Test
	public void projectionConverterEnabled() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-converter-enabled[]
			List<OrderStatus> result = searchSession.search( Order.class )
					.asProjection( f -> f.field( "status", OrderStatus.class ) )
					.predicate( f -> f.matchAll() )
					.fetchHits();
			// end::projection-converter-enabled[]

			assertThat( result )
					.containsExactlyInAnyOrder( OrderStatus.values() );
		} );
	}

	@Test
	public void projectionConverterDisabled() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-converter-disabled[]
			List<String> result = searchSession.search( Order.class )
					.asProjection( f -> f.field( "status", String.class, ProjectionConverter.DISABLED ) )
					.predicate( f -> f.matchAll() )
					.fetchHits();
			// end::projection-converter-disabled[]

			assertThat( result )
					.containsExactlyInAnyOrder(
							Stream.of( OrderStatus.values() ).map( Enum::name ).toArray( String[]::new )
					);
		} );
	}

	private void initData() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Order order1 = new Order( 1 );
			order1.setStatus( OrderStatus.ACKNOWLEDGED );
			Order order2 = new Order( 2 );
			order2.setStatus( OrderStatus.IN_PROGRESS );
			Order order3 = new Order( 3 );
			order3.setStatus( OrderStatus.DELIVERED );

			entityManager.persist( order1 );
			entityManager.persist( order2 );
			entityManager.persist( order3 );
		} );
	}

	@Entity(name = "Order")
	@Table(name = "orders")
	@Indexed
	public static class Order {
		@Id
		private Integer id;
		@Basic
		@Enumerated
		@KeywordField(projectable = Projectable.YES)
		private OrderStatus status;

		protected Order() {
			// For Hibernate ORM
		}

		public Order(int id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public OrderStatus getStatus() {
			return status;
		}

		public void setStatus(OrderStatus status) {
			this.status = status;
		}
	}

	private enum OrderStatus {
		ACKNOWLEDGED,
		IN_PROGRESS,
		DELIVERED
	}

}
