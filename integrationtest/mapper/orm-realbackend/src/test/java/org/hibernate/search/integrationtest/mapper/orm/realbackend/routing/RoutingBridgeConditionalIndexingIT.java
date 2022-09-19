/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;

import org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test that defining a routing bridge that generates (mutable) routing keys works as expected
 * with a full setup (real mapper + real backend).
 */
public class RoutingBridgeConditionalIndexingIT {

	@Rule
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty( HibernateOrmMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY,
						IndexingPlanSynchronizationStrategyNames.READ_SYNC )
				.setup( Book.class );
	}

	@Test
	public void testLifecycle() {
		assertThat( searchAllIds() ).isEmpty();

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book1 = new Book();
			book1.setId( 1 );
			book1.setTitle( "I, Robot" );
			book1.setStatus( Status.DRAFT );
			entityManager.persist( book1 );
		} );

		assertThat( searchAllIds() ).isEmpty();

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book1 = entityManager.getReference( Book.class, 1 );
			book1.setStatus( Status.PUBLISHED );
		} );

		assertThat( searchAllIds() ).containsExactly( 1 );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book1 = entityManager.getReference( Book.class, 1 );
			// Try to fool the routing bridge into not deleting - it shouldn't matter.
			book1.setStatus( Status.ARCHIVED );
			entityManager.remove( book1 );
		} );

		assertThat( searchAllIds() ).isEmpty();
	}

	private List<Integer> searchAllIds() {
		List<Integer> results = new ArrayList<>();
		with( entityManagerFactory ).runInTransaction( entityManager -> Search.session( entityManager )
				.search( Book.class )
				.select( f -> f.id( Integer.class ) )
				.where( f -> f.matchAll() )
				.fetchAllHits()
				.stream()
				.forEach( results::add ) );
		return results;
	}

	public static class BookStatusRoutingBinder implements RoutingBinder {

		@Override
		public void bind(RoutingBindingContext context) {
			context.dependencies()
					.use( "status" );

			context.bridge(
					Book.class,
					new Bridge()
			);
		}

		public static class Bridge implements RoutingBridge<Book> {
			@Override
			public void route(DocumentRoutes routes, Object entityIdentifier, Book indexedEntity,
					RoutingBridgeRouteContext context) {
				switch ( indexedEntity.getStatus() ) {
					case PUBLISHED:
						routes.addRoute();
						break;
					case DRAFT:
					case ARCHIVED:
						routes.notIndexed();
						break;
				}
			}

			@Override
			public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, Book indexedEntity,
					RoutingBridgeRouteContext context) {
				routes.addRoute();
			}
		}
	}

	@Entity(name = "book")
	@Indexed(routingBinder = @RoutingBinderRef(type = BookStatusRoutingBinder.class))
	public static class Book {

		@Id
		private Integer id;

		private String title;

		@Basic(optional = false)
		@KeywordField
		private Status status;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public Status getStatus() {
			return status;
		}

		public void setStatus(Status status) {
			this.status = status;
		}
	// end::getters-setters[]
	}

	public enum Status {

		DRAFT,
		PUBLISHED,
		ARCHIVED

	}
}
