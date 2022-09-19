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
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test that defining a routing bridge that generates (mutable) routing keys works as expected
 * with a full setup (real mapper + real backend).
 */
public class RoutingBridgeRoutingKeyIT {

	// Use high enough shard count that it's unlikely that our two routing keys end up in the same shard
	private static final int SHARD_COUNT = 64;

	@Rule
	public OrmSetupHelper setupHelper =
			OrmSetupHelper.withSingleBackend( BackendConfigurations.hashBasedSharding( SHARD_COUNT ) );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty( HibernateOrmMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY,
						IndexingPlanSynchronizationStrategyNames.READ_SYNC )
				// This call in testLifecycle to Elasticsearch sever may exceed 30 seconds,
				// from some machine. This is a server (not Hibernate Search) latency.
				// E.g: > query parameters {} and 1 objects in payload in 30617ms.
				.withProperty( "hibernate.search.backend.read_timeout", "60000" )
				.setup( Book.class );
	}

	@Test
	public void testLifecycle() {
		assertThat( searchIdsByRoutingKey( Genre.SCIENCE_FICTION ) ).isEmpty();
		assertThat( searchIdsByRoutingKey( Genre.CRIME_FICTION ) ).isEmpty();

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book1 = new Book();
			book1.setId( 1 );
			book1.setTitle( "I, Robot" );
			book1.setGenre( Genre.SCIENCE_FICTION );
			entityManager.persist( book1 );
		} );

		assertThat( searchIdsByRoutingKey( Genre.SCIENCE_FICTION ) ).containsExactly( 1 );
		assertThat( searchIdsByRoutingKey( Genre.CRIME_FICTION ) ).isEmpty();

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book1 = entityManager.getReference( Book.class, 1 );
			book1.setGenre( Genre.CRIME_FICTION );
		} );

		assertThat( searchIdsByRoutingKey( Genre.SCIENCE_FICTION ) ).isEmpty();
		assertThat( searchIdsByRoutingKey( Genre.CRIME_FICTION ) ).containsExactly( 1 );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book1 = entityManager.getReference( Book.class, 1 );
			// Try to fool the routing bridge into deleting from the wrong shard - it shouldn't matter.
			book1.setGenre( Genre.SCIENCE_FICTION );
			entityManager.remove( book1 );
		} );

		assertThat( searchIdsByRoutingKey( Genre.SCIENCE_FICTION ) ).isEmpty();
		assertThat( searchIdsByRoutingKey( Genre.CRIME_FICTION ) ).isEmpty();
	}

	private List<Integer> searchIdsByRoutingKey(Genre genre) {
		String routingKey = genre.name();
		List<Integer> results = new ArrayList<>();
		with( entityManagerFactory ).runInTransaction( entityManager -> Search.session( entityManager )
				.search( Book.class )
				.select( f -> f.id( Integer.class ) )
				.where( f -> f.matchAll() )
				.routing( routingKey )
				.fetchAllHits()
				.stream()
				.forEach( results::add ) );
		return results;
	}

	@Entity(name = "book")
	@Indexed(routingBinder = @RoutingBinderRef(type = BookGenreRoutingBinder.class))
	public static class Book {
		@Id
		private Integer id;

		private String title;

		@Basic(optional = false)
		private Genre genre;

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

		public Genre getGenre() {
			return genre;
		}

		public void setGenre(Genre genre) {
			this.genre = genre;
		}
	}

	public enum Genre {
		SCIENCE_FICTION,
		CRIME_FICTION
	}

	public static class BookGenreRoutingBinder implements RoutingBinder {

		@Override
		public void bind(RoutingBindingContext context) {
			context.dependencies()
					.use( "genre" );

			context.bridge( Book.class, new Bridge() );
		}

		public static class Bridge implements RoutingBridge<Book> {
			@Override
			public void route(DocumentRoutes routes, Object entityIdentifier, Book indexedEntity,
					RoutingBridgeRouteContext context) {
				String routingKey = indexedEntity.getGenre().name();
				routes.addRoute().routingKey( routingKey );
			}

			@Override
			public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, Book indexedEntity,
					RoutingBridgeRouteContext context) {
				for ( Genre possiblePreviousGenre : Genre.values() ) {
					String routingKey = possiblePreviousGenre.name();
					routes.addRoute().routingKey( routingKey );
				}
			}
		}
	}
}
