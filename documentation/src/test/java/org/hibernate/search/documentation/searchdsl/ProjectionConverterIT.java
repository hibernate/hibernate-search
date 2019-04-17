/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.searchdsl;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.search.documentation.testsupport.BackendSetupStrategy;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.projection.ProjectionConverter;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
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
	public static Object[] backendSetups() {
		return BackendSetupStrategy.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper = new OrmSetupHelper();

	private final BackendSetupStrategy backendSetupStrategy;

	private EntityManagerFactory entityManagerFactory;

	public ProjectionConverterIT(BackendSetupStrategy backendSetupStrategy) {
		this.backendSetupStrategy = backendSetupStrategy;
	}

	@Before
	public void setup() {
		entityManagerFactory = backendSetupStrategy.withSingleBackend( setupHelper ).setup( Order.class );
		initData();
	}

	@Test
	public void projectionConverterEnabled() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.getSearchSession( entityManager );

			// tag::projection-converter-enabled[]
			SearchQuery<OrderStatus> query = searchSession.search( Order.class )
					.asProjection( f -> f.field( "status", OrderStatus.class ) )
					.predicate( f -> f.matchAll() )
					.toQuery();
			// end::projection-converter-enabled[]

			List<OrderStatus> result = query.fetchHits();

			assertThat( result )
					.containsExactlyInAnyOrder( OrderStatus.values() );
		} );
	}

	@Test
	public void projectionConverterDisabled() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.getSearchSession( entityManager );

			// tag::projection-converter-disabled[]
			SearchQuery<String> query = searchSession.search( Order.class )
					.asProjection( f -> f.field( "status", String.class, ProjectionConverter.DISABLED ) )
					.predicate( f -> f.matchAll() )
					.toQuery();
			// end::projection-converter-disabled[]

			List<String> result = query.fetchHits();

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
