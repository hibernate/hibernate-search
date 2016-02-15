/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.search.genericjpa.Constants;
import org.hibernate.search.genericjpa.JPASearchFactoryController;
import org.hibernate.search.genericjpa.Setup;
import org.hibernate.search.genericjpa.db.events.triggers.TriggerSQLStringSource;
import org.hibernate.search.genericjpa.entity.EntityManagerEntityProviderAdapter;
import org.hibernate.search.genericjpa.impl.JPASearchFactoryAdapter;
import org.hibernate.search.genericjpa.test.jpa.entities.CustomUpdatedEntity;
import org.hibernate.search.genericjpa.test.jpa.entities.CustomUpdatedEntityEntityProvider;
import org.hibernate.search.genericjpa.test.jpa.entities.MultipleColumnsIdEntity;
import org.hibernate.search.genericjpa.test.jpa.entities.NonJPAEntity;
import org.hibernate.search.genericjpa.util.Sleep;
import org.hibernate.search.jpa.FullTextEntityManager;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Martin on 10.07.2015.
 */
public abstract class AutomaticUpdatesIntegrationTest {


	protected EntityManagerFactory emf;
	protected EntityManager em;
	protected Class<? extends TriggerSQLStringSource> triggerSourceClass;
	protected String searchFactoryType;

	private static final int COUNT_MULTIPLE_COLUMNS = 100;

	@Test
	public void testMultipleColumnsIdEntity() throws InterruptedException {
		Properties properties = new Properties();
		properties.setProperty( "org.hibernate.search.genericjpa.searchfactory.name", "testCustomUpdatedEntity" );
		properties.setProperty(
				"hibernate.search.trigger.source",
				this.triggerSourceClass.getName()
		);
		properties.setProperty(
				Constants.TRIGGER_CREATION_STRATEGY_KEY,
				Constants.TRIGGER_CREATION_STRATEGY_DROP_CREATE
		);
		properties.setProperty( "hibernate.search.searchfactory.type", this.searchFactoryType );
		JPASearchFactoryController searchController = Setup.createSearchFactoryController( this.emf, properties );
		try {

			FullTextEntityManager fem = searchController.getFullTextEntityManager( this.em );
			this.em.getTransaction().begin();
			for ( int i = 0; i < COUNT_MULTIPLE_COLUMNS; ++i ) {
				MultipleColumnsIdEntity ent = new MultipleColumnsIdEntity();
				ent.setFirstId( "first" + i );
				ent.setSecondId( "second" + i );
				ent.setInfo( "info" + i );
				this.em.persist( ent );
			}
			this.em.getTransaction().commit();
			this.em.clear();

			Sleep.sleep(
					100_000,
					() -> COUNT_MULTIPLE_COLUMNS == searchController.getFullTextEntityManager( em ).createFullTextQuery(
							new MatchAllDocsQuery(),
							MultipleColumnsIdEntity.class
					).getResultSize()
					,
					100,
					""
			);
		}
		finally {
			searchController.close();
		}
	}

	private static final int ENTITY_COUNT_CUSTOM = 100;
	private static final String ENT_NAME_SHOULD_NOT_FIND_CUSTOM = "shouldnot";

	@Test
	public void testCustomUpdatedEntity() throws InterruptedException {
		if ( !"sql".equals( this.searchFactoryType ) ) {
			System.out.println( "skipping custom updated entity test for searchFactoryType: " + this.searchFactoryType );
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
		try {
			{
				EntityManager em = emf.createEntityManager();
				try {
					em.getTransaction().begin();
					for ( int i = 1; i < ENTITY_COUNT_CUSTOM + 1; ++i ) {
						CustomUpdatedEntity ent = new CustomUpdatedEntity();
						ent.setId( (long) i );
						ent.setText( ENT_NAME_SHOULD_NOT_FIND_CUSTOM );
						em.persist( ent );
					}
					em.getTransaction().commit();
				}
				catch (Exception e) {
					em.getTransaction().rollback();
				}
				finally {
					em.close();
				}
			}

			Sleep.sleep(
					100_000,
					() -> ENTITY_COUNT_CUSTOM == searchFactory.getFullTextEntityManager( this.em )
							.createFullTextQuery( new MatchAllDocsQuery(), CustomUpdatedEntity.class )
							.getResultSize()
			);

			//the original name should not be found
			assertEquals(
					0,
					searchFactory.getFullTextEntityManager( this.em )
							.createFullTextQuery(
									searchFactory.getSearchFactory()
											.buildQueryBuilder()
											.forEntity(
													CustomUpdatedEntity.class
											)
											.get()
											.keyword()
											.onField( "text" )
											.matching( ENT_NAME_SHOULD_NOT_FIND_CUSTOM )
											.createQuery()
							).getResultSize()
			);

			//but the entities should still be the same
			for ( CustomUpdatedEntity ent : (List<CustomUpdatedEntity>) searchFactory.getFullTextEntityManager( this.em )
					.createFullTextQuery(
							searchFactory.getSearchFactory()
									.buildQueryBuilder()
									.forEntity(
											CustomUpdatedEntity.class
									)
									.get()
									.keyword()
									.onField( "text" )
									.matching( ENT_NAME_SHOULD_NOT_FIND_CUSTOM )
									.createQuery()
					).getResultList() ) {
				assertEquals( ENT_NAME_SHOULD_NOT_FIND_CUSTOM, ent.getText() );
			}

			this.assertEveryThingThereHintsCustomUpdated( searchFactory );

			searchFactory.getFullTextEntityManager( this.em )
					.createIndexer( CustomUpdatedEntity.class )
					.entityProvider(
							EntityManagerEntityProviderAdapter.adapt(
									CustomUpdatedEntityEntityProvider.class,
									this.em,
									null
							)
					).startAndWait();
			this.assertEveryThingThereCustomUpdated( searchFactory );

			searchFactory.getFullTextEntityManager( this.em )
					.createIndexer( CustomUpdatedEntity.class )
					.entityProvider(
							EntityManagerEntityProviderAdapter.adapt(
									CustomUpdatedEntityEntityProvider.class,
									this.emf,
									null, 8
							)
					).startAndWait();
			this.assertEveryThingThereCustomUpdated( searchFactory );
		}
		finally {
			searchFactory.close();
		}
	}

	private void assertEveryThingThereHintsCustomUpdated(JPASearchFactoryController searchFactory) {
		assertEquals(
				ENTITY_COUNT_CUSTOM, searchFactory.getFullTextEntityManager( this.em )
						.createFullTextQuery(
								searchFactory.getSearchFactory()
										.buildQueryBuilder()
										.forEntity(
												CustomUpdatedEntity.class
										)
										.get()
										.keyword()
										.onField( "text" )
										.matching( CustomUpdatedEntityEntityProvider.CUSTOM_TEXT_HINTS )
										.createQuery()
						).getResultSize()
		);
	}

	private void assertEveryThingThereCustomUpdated(JPASearchFactoryController searchFactory) {
		assertEquals(
				ENTITY_COUNT_CUSTOM, searchFactory.getFullTextEntityManager( this.em )
						.createFullTextQuery(
								searchFactory.getSearchFactory()
										.buildQueryBuilder()
										.forEntity(
												CustomUpdatedEntity.class
										)
										.get()
										.keyword()
										.onField( "text" )
										.matching( CustomUpdatedEntityEntityProvider.CUSTOM_TEXT )
										.createQuery()
						).getResultSize()
		);
	}

	public void setup(
			String searchFactoryType,
			String persistenceUnit,
			Class<? extends TriggerSQLStringSource> triggerSourceClass) {
		this.emf = Persistence.createEntityManagerFactory( persistenceUnit );
		this.em = emf.createEntityManager();
		this.triggerSourceClass = triggerSourceClass;
		this.searchFactoryType = searchFactoryType;
	}

	@After
	public void shutdown() {
		if ( this.em != null ) {
			try {
				this.em.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		if ( this.emf != null ) {
			try {
				this.emf.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
