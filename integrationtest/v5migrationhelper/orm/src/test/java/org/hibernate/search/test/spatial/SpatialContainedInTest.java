/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.spatial;

import static org.junit.Assert.assertEquals;

import java.util.List;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Latitude;
import org.hibernate.search.annotations.Longitude;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.Test;

/**
 * Test behavior when using {@code @ContainedIn} in a {@code @Spatial}-annotated entity.
 *
 * @author Yoann Rodiere
 */
public class SpatialContainedInTest extends SearchInitializationTestBase {

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2858")
	public void test() throws Exception {
		// We mainly want to test that the initialization won't fail with an NPE
		init( Actor.class, ActorLocation.class );

		/*
		 * The tests below are just to check that initialization actually took spatial into account.
		 * In order words, we check that HSEARCH-2858 wasn't fixed simply because @Spatial are
		 * not effective anymore...
		 */

		try ( Session session = getTestResourceManager().openSession() ) {
			Actor actor1 = new Actor();
			actor1.setId( 1L );
			ActorLocation location = new ActorLocation();
			location.setId( 2L );
			location.setLat( 24.0d );
			location.setLon( 31.5d );
			actor1.setActorLocation( location );
			location.setActor( actor1 );

			Transaction tx = session.beginTransaction();
			session.persist( location );
			session.persist( actor1 );
			tx.commit();
		}

		// Check that a distance query will return the actor
		try ( Session session = getTestResourceManager().openSession();
				FullTextSession ftSession = Search.getFullTextSession( session ) ) {

			final QueryBuilder builder = ftSession.getSearchFactory()
					.buildQueryBuilder().forEntity( Actor.class ).get();

			org.apache.lucene.search.Query luceneQuery = builder.spatial()
					.onField( "actorLocation.location" )
					.within( 100.0d, Unit.KM )
					.ofLatitude( 24.0d )
					.andLongitude( 31.5d )
					.createQuery();

			FullTextQuery query = ftSession.createFullTextQuery( luceneQuery, Actor.class );
			List<?> results = query.list();
			assertEquals( 1, results.size() );
		}

		// Change the location so that it doesn't match the query anymore
		try ( Session session = getTestResourceManager().openSession() ) {
			ActorLocation location = session.get( ActorLocation.class, 2L );
			location.setLat( -24.0d );
			location.setLon( -31.5d );

			Transaction tx = session.beginTransaction();
			session.save( location );
			tx.commit();
		}

		// Check that the actor isn't returned by the query anymore, i.e. that @ContainedIn actually worked
		try ( Session session = getTestResourceManager().openSession();
				FullTextSession ftSession = Search.getFullTextSession( session ) ) {

			final QueryBuilder builder = ftSession.getSearchFactory()
					.buildQueryBuilder().forEntity( Actor.class ).get();

			org.apache.lucene.search.Query luceneQuery = builder.spatial()
					.onField( "actorLocation.location" )
					.within( 100.0d, Unit.KM )
					.ofLatitude( 24.0d )
					.andLongitude( 31.5d )
					.createQuery();

			FullTextQuery query = ftSession.createFullTextQuery( luceneQuery, Actor.class );
			List<?> results = query.list();
			assertEquals( 0, results.size() );
		}
	}

	@Entity
	@Indexed
	public static class Actor {

		@Id
		private Long id;

		@Embedded
		@IndexedEmbedded
		@OneToOne
		private ActorLocation actorLocation;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public ActorLocation getActorLocation() {
			return actorLocation;
		}

		public void setActorLocation(ActorLocation actorLocation) {
			this.actorLocation = actorLocation;
		}
	}

	@Entity
	@Spatial(name = "location")
	public static class ActorLocation {

		@Id
		private Long id;

		@OneToOne(mappedBy = "actorLocation")
		private Actor actor;

		@Latitude(of = "location")
		private Double lat;

		@Longitude(of = "location")
		private Double lon;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Actor getActor() {
			return actor;
		}

		public void setActor(Actor actor) {
			this.actor = actor;
		}

		public Double getLat() {
			return lat;
		}

		public void setLat(Double lat) {
			this.lat = lat;
		}

		public Double getLon() {
			return lon;
		}

		public void setLon(Double lon) {
			this.lon = lon;
		}

	}

}
