/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.converter;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.stream.Stream;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ProjectionConverterIT {
	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Order.class );
		initData();
	}

	@Test
	public void projectionConverterEnabled() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-converter-enabled[]
			List<OrderStatus> result = searchSession.search( Order.class )
					.select( f -> f.field( "status", OrderStatus.class ) )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::projection-converter-enabled[]

			assertThat( result )
					.containsExactlyInAnyOrder( OrderStatus.values() );
		} );
	}

	@Test
	public void projectionConverterDisabled() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::projection-converter-disabled[]
			List<String> result = searchSession.search( Order.class )
					.select( f -> f.field( "status", String.class, ValueConvert.NO ) )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			// end::projection-converter-disabled[]

			assertThat( result )
					.containsExactlyInAnyOrder(
							Stream.of( OrderStatus.values() ).map( Enum::name ).toArray( String[]::new )
					);
		} );
	}

	private void initData() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
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
