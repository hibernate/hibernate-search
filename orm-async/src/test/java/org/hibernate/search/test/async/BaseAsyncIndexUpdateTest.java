/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.async;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.triggers.impl.TriggerServiceConstants;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.genericjpa.util.Sleep;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.entities.Domain;
import org.hibernate.search.test.entities.Embedded;
import org.hibernate.search.test.entities.OverrideEntity;
import org.hibernate.search.test.entities.Place;
import org.hibernate.search.test.entities.SecondaryTableEntity;
import org.hibernate.search.test.entities.Sorcerer;
import org.hibernate.search.test.entities.TopLevel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Martin Braun
 */
public abstract class BaseAsyncIndexUpdateTest extends SearchTestBase {

	private final boolean isProfileTest;
	private boolean skipProfileTests = false;

	protected BaseAsyncIndexUpdateTest(boolean isProfileTest) {
		this.isProfileTest = isProfileTest;
	}

	protected Session session;

	protected abstract void setup();

	protected abstract void shutdown();

	@Before
	public final void setupTest() {
		this.session = this.getSessionFactory().openSession();
		this.setup();
	}

	@After
	public final void afterTest() {
		this.session.close();
		this.shutdown();
	}

	@Test
	public void testSingularEntity() throws InterruptedException {
		if ( this.isProfileTest && this.skipProfileTests ) {
			System.out.println( "skipping this test for the selected profile" );
			return;
		}

		this.session.getTransaction().begin();

		Domain domain = new Domain();
		domain.setId( 1 );
		domain.setName( "toast.de" );
		this.session.persist( domain );

		this.session.getTransaction().commit();

		this.assertCount( Domain.class, 1, new MatchAllDocsQuery() );
	}

	@Test
	public void testEmbedded() throws InterruptedException {
		if ( this.isProfileTest && this.skipProfileTests ) {
			System.out.println( "skipping this test for the selected profile" );
			return;
		}

		{
			this.session.getTransaction().begin();

			TopLevel topLevel = new TopLevel();
			topLevel.setId( "ID" );
			this.session.persist( topLevel );

			this.session.getTransaction().commit();

			this.assertCount( TopLevel.class, 1, new MatchAllDocsQuery() );
		}

		{
			this.session.getTransaction().begin();
			TopLevel topLevel = this.session.get( TopLevel.class, "ID" );

			Embedded embedded = new Embedded();
			embedded.setSomeValue( "toast" );
			topLevel.setEmbedded( new HashSet<>( Collections.singletonList( embedded ) ) );

			this.session.merge( topLevel );

			this.session.getTransaction().commit();

			this.assertCount( TopLevel.class, 1, new TermQuery( new Term( "embedded.someValue", "toast" ) ) );
		}
	}

	@Test
	public void testSecondaryTable() throws InterruptedException {
		if ( this.isProfileTest && this.skipProfileTests ) {
			System.out.println( "skipping this test for the selected profile" );
			return;
		}

		{
			this.session.getTransaction().begin();

			SecondaryTableEntity entity = new SecondaryTableEntity();
			entity.setId( 1L );
			entity.setSecondary( "secondary" );
			this.session.persist( entity );

			this.session.getTransaction().commit();

			this.assertCount( SecondaryTableEntity.class, 1, new TermQuery( new Term( "secondary", "secondary" ) ) );
		}

		{
			this.session.getTransaction().begin();

			SecondaryTableEntity entity = this.session.get( SecondaryTableEntity.class, 1L );
			entity.setSecondary( "toast" );
			this.session.merge( entity );

			this.session.getTransaction().commit();

			this.assertCount( SecondaryTableEntity.class, 1, new TermQuery( new Term( "secondary", "toast" ) ) );
		}
	}

	@Test
	public void testBasicOneToManyMapping() throws InterruptedException {
		if ( this.isProfileTest && this.skipProfileTests ) {
			System.out.println( "skipping this test for the selected profile" );
			return;
		}

		{
			this.session.getTransaction().begin();

			Place place = new Place();
			place.setId( 1 );
			place.setName( "helmsdeep" );
			this.session.persist( place );

			this.session.getTransaction().commit();

			this.assertCount( Place.class, 1, new TermQuery( new Term( "name", "helmsdeep" ) ) );
		}

		{
			this.session.getTransaction().begin();

			Place place = this.session.get( Place.class, 1 );

			Sorcerer sorcerer = new Sorcerer();
			sorcerer.setId( 2 );
			sorcerer.setName( "gandalf" );
			sorcerer.setPlace( place );
			place.setSorcerers( new HashSet<>( Collections.singletonList( sorcerer ) ) );

			this.session.persist( sorcerer );

			this.session.getTransaction().commit();

			this.assertCount( Place.class, 1, new TermQuery( new Term( "sorcerers.name", "gandalf" ) ) );
		}
	}

	private void assertCount(Class<?> entityClass, int count, Query query) throws InterruptedException {
		Sleep.sleep(
				100_000, () -> {
					this.session.getTransaction().begin();
					FullTextSession fullTextSession = Search.getFullTextSession( this.session );
					FullTextQuery ftQuery = fullTextSession.createFullTextQuery(
							query,
							entityClass
					);
					boolean ret = ftQuery.getResultSize() == count;
					this.session.getTransaction().commit();
					return ret;
				}
				, 100, "index didn't update properly"
		);
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Domain.class,
				OverrideEntity.class,
				Place.class,
				SecondaryTableEntity.class,
				Sorcerer.class,
				TopLevel.class,
				Embedded.class
		};
	}

	@Override
	public void configure(Map<String, Object> cfg) {
		super.configure( cfg );
		// for this test we explicitly set the auto commit mode since we are not explicitly starting a transaction
		// which could be a problem in some databases.
		cfg.put( "hibernate.connection.autocommit", "true" );

		//cfg.put( "hibernate.search.default.directory_provider", "filesystem" );
		//cfg.put( "hibernate.search.default.indexBase", "indexes" );

		cfg.put( "hibernate.search.indexing_strategy", "manual" );

		Properties hibernateProperties = new Properties();
		try (InputStream is = BaseAsyncIndexUpdateTest.class.getResourceAsStream( "/hibernate.properties" )) {
			hibernateProperties.load( is );
		}
		catch (IOException e) {
			throw new AssertionFailure( "unexpected Exception", e );
		}
		this.skipProfileTests = hibernateProperties.getProperty( "skipProfileTests", "false" ).equals( "true" );
		if ( !(this.skipProfileTests && this.isProfileTest) ) {
			cfg.put( TriggerServiceConstants.TRIGGER_BASED_BACKEND_KEY, "true" );
		}
	}

}
