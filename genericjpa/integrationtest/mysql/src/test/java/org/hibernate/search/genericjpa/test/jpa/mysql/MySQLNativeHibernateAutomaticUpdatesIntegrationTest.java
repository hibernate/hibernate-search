/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.jpa.mysql;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.search.genericjpa.Constants;
import org.hibernate.search.genericjpa.Setup;
import org.hibernate.search.genericjpa.db.events.UpdateConsumer;
import org.hibernate.search.genericjpa.db.events.triggers.MySQLTriggerSQLStringSource;
import org.hibernate.search.genericjpa.impl.JPASearchFactoryAdapter;
import org.hibernate.search.genericjpa.test.jpa.AutomaticUpdatesIntegrationTest;
import org.hibernate.search.genericjpa.test.jpa.entities.ID;
import org.hibernate.search.genericjpa.test.jpa.entities.MultipleColumnsIdEntity;
import org.hibernate.search.genericjpa.test.jpa.entities.NonJPAEntity;
import org.hibernate.search.genericjpa.test.jpa.entities.Place;
import org.hibernate.search.genericjpa.test.jpa.entities.Sorcerer;
import org.hibernate.search.jpa.FullTextEntityManager;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Martin on 28.07.2015.
 */
public class MySQLNativeHibernateAutomaticUpdatesIntegrationTest extends AutomaticUpdatesIntegrationTest {

	@Before
	public void setup() {
		this.setup( "hibernate", "Hibernate_MySQL", MySQLTriggerSQLStringSource.class );
	}

	//TODO: test this for object hierarchies

	@Test
	public void testNativeEvents() {
		if ( "sql".equals( this.searchFactoryType ) ) {
			System.out.println( "skipping native event test for searchFactoryType (useless for this type): " + this.searchFactoryType );
			return;
		}
		Properties properties = new Properties();
		properties.setProperty( Constants.SEARCH_FACTORY_NAME_KEY, "testCustomUpdatedEntity" );
		properties.setProperty( Constants.ADDITIONAL_INDEXED_TYPES_KEY, NonJPAEntity.class.getName() );
		//we do manual updates, so this will be ignored, but let's keep it here
		//if we change our mind later
		properties.setProperty(
				Constants.TRIGGER_SOURCE_KEY,
				this.triggerSourceClass.getName()
		);
		properties.setProperty(
				Constants.TRIGGER_CREATION_STRATEGY_KEY,
				Constants.TRIGGER_CREATION_STRATEGY_DROP_CREATE
		);
		properties.setProperty( Constants.BATCH_SIZE_FOR_UPDATES_KEY, "2" );
		properties.setProperty( Constants.SEARCH_FACTORY_TYPE_KEY, this.searchFactoryType );
		JPASearchFactoryAdapter searchFactory = (JPASearchFactoryAdapter) Setup.createSearchFactoryController(
				this.emf,
				properties
		);

		final boolean[] receivedEvent = {false};
		searchFactory.addUpdateConsumer(
				new UpdateConsumer() {
					@Override
					public void updateEvent(List<UpdateEventInfo> updateInfo) {
						receivedEvent[0] = true;
					}
				}
		);

		try {
			for ( int times = 0; times < 100; ++times ) {
				this.em.getTransaction().begin();
				try {
					for ( int i = 0; i < 5; ++i ) {
						MultipleColumnsIdEntity ent = new MultipleColumnsIdEntity();
						ent.setFirstId( "first" + i );
						ent.setSecondId( "second" + i );
						ent.setInfo( "info" + i );
						this.em.persist( ent );
						this.em.flush();
					}
				}
				finally {
					this.em.getTransaction().rollback();
				}
			}
			assertEquals(
					0, searchFactory.getFullTextEntityManager( this.em )
							.createFullTextQuery( new MatchAllDocsQuery(), MultipleColumnsIdEntity.class )
							.getResultSize()
			);

			{
				this.em.getTransaction().begin();
				try {
					MultipleColumnsIdEntity ent = new MultipleColumnsIdEntity();
					ent.setFirstId( "first" );
					ent.setSecondId( "second" );
					ent.setInfo( "info" );
					this.em.persist( ent );
					this.em.flush();
					this.em.getTransaction().commit();
				}
				catch (Exception e) {
					this.em.getTransaction().rollback();
					throw e;
				}

				assertEquals(
						1, searchFactory.getFullTextEntityManager( this.em )
								.createFullTextQuery( new MatchAllDocsQuery(), MultipleColumnsIdEntity.class )
								.getResultSize()
				);
			}

			{
				this.em.getTransaction().begin();
				try {
					MultipleColumnsIdEntity ent = this.em.find(
							MultipleColumnsIdEntity.class, new ID(
									"first",
									"second"
							)
					);
					ent.setInfo( "info_new" );
					this.em.getTransaction().commit();
				}
				catch (Exception e) {
					this.em.getTransaction().rollback();
					throw e;
				}

				assertEquals(
						1, searchFactory.getFullTextEntityManager( this.em )
								.createFullTextQuery( new MatchAllDocsQuery(), MultipleColumnsIdEntity.class )
								.getResultSize()
				);

				FullTextEntityManager fem = searchFactory.getFullTextEntityManager( this.em );

				assertEquals(
						1, fem.createFullTextQuery(
								fem.getSearchFactory().buildQueryBuilder().forEntity(
										MultipleColumnsIdEntity.class
								).get().keyword().onField( "info" ).matching( "info_new" ).createQuery(),
								MultipleColumnsIdEntity.class
						).getResultSize()
				);
			}

			{
				this.em.getTransaction().begin();
				try {
					MultipleColumnsIdEntity ent = this.em.find(
							MultipleColumnsIdEntity.class, new ID(
									"first",
									"second"
							)
					);
					this.em.remove( ent );
					this.em.flush();
					this.em.getTransaction().commit();
				}
				catch (Exception e) {
					this.em.getTransaction().rollback();
					throw e;
				}

				assertEquals(
						0, searchFactory.getFullTextEntityManager( this.em )
								.createFullTextQuery( new MatchAllDocsQuery(), MultipleColumnsIdEntity.class )
								.getResultSize()
				);
			}

			{
				this.em.getTransaction().begin();
				try {
					Place place = new Place();
					place.setCool( true );
					place.setName( "name" );
					Sorcerer sorcerer = new Sorcerer();
					sorcerer.setName( "sorcname" );
					sorcerer.setPlace( place );
					place.setSorcerers( new HashSet<>( Arrays.asList( sorcerer ) ) );
					this.em.persist( place );
					this.em.flush();
					this.em.getTransaction().commit();
				}
				catch (Exception e) {
					this.em.getTransaction().rollback();
					throw e;
				}

				assertEquals(
						1, searchFactory.getFullTextEntityManager( this.em )
								.createFullTextQuery( new MatchAllDocsQuery(), Place.class )
								.getResultSize()
				);
			}

			{
				this.em.getTransaction().begin();
				try {
					Sorcerer sorc = (Sorcerer) this.em.createQuery( "SELECT a FROM Sorcerer a" )
							.getResultList()
							.get( 0 );
					sorc.setName( "newname" );
					this.em.getTransaction().commit();
				}
				catch (Exception e) {
					this.em.getTransaction().rollback();
					throw e;
				}

				assertEquals(
						1, searchFactory.getFullTextEntityManager( this.em )
								.createFullTextQuery( new MatchAllDocsQuery(), Place.class )
								.getResultSize()
				);

				FullTextEntityManager fem = searchFactory.getFullTextEntityManager( this.em );

				assertEquals(
						1, fem.createFullTextQuery(
								fem.getSearchFactory()
										.buildQueryBuilder()
										.forEntity( Place.class )
										.get()
										.keyword()
										.onField( "sorcerers.name" )
										.matching( "newname" ).createQuery(), Place.class
						).getResultSize()
				);

				assertEquals(
						0, fem.createFullTextQuery(
								fem.getSearchFactory()
										.buildQueryBuilder()
										.forEntity( Place.class )
										.get()
										.keyword()
										.onField( "sorcerers.name" )
										.matching( "sorcname" ).createQuery(), Place.class
						).getResultSize()
				);
			}

			{
				this.em.getTransaction().begin();
				try {
					Sorcerer sorc = (Sorcerer) this.em.createQuery( "SELECT a FROM Sorcerer a" )
							.getResultList()
							.get( 0 );
					sorc.setName( "sorcname" );
					this.em.flush();
				}
				finally {
					this.em.getTransaction().rollback();
				}

				FullTextEntityManager fem = searchFactory.getFullTextEntityManager( this.em );

				assertEquals(
						0, fem.createFullTextQuery(
								fem.getSearchFactory()
										.buildQueryBuilder()
										.forEntity( Place.class )
										.get()
										.keyword()
										.onField( "sorcerers.name" )
										.matching( "sorcname" ).createQuery(), Place.class
						).getResultSize()
				);
			}
			assertTrue( receivedEvent[0] );
		}
		finally {
			searchFactory.close();
		}
	}

}
