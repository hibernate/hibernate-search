/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.async;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.triggers.impl.TriggerServiceConstants;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.genericjpa.util.Sleep;
import org.hibernate.search.test.SearchTestBase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Martin on 12.11.2015.
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
	public void test() {
		if ( this.isProfileTest && this.skipProfileTests ) {
			System.out.println("skipping this test for the selected profile");
			return;
		}

		this.session.getTransaction().begin();

		Domain domain = new Domain();
		domain.setId( 1 );
		domain.setName( "toast.de" );
		this.session.persist( domain );

		this.session.getTransaction().commit();

		try {
			Sleep.sleep(
					100_000, () -> {
						this.session.getTransaction().begin();
						FullTextSession fullTextSession = Search.getFullTextSession( session );
						FullTextQuery ftQuery = fullTextSession.createFullTextQuery(
								new MatchAllDocsQuery(),
								Domain.class
						);
						boolean ret = ftQuery.getResultSize() == 1;
						this.session.getTransaction().commit();
						return ret;
					}
					, 100, "index didn't update properly"
			);
		}
		catch (InterruptedException e) {
			throw new AssertionError( e );
		}

	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Domain.class
		};
	}

	@Override
	public void configure(Map<String, Object> cfg) {
		super.configure( cfg );
		// for this test we explicitly set the auto commit mode since we are not explicitly starting a transaction
		// which could be a problem in some databases.
		cfg.put( "hibernate.connection.autocommit", "true" );

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
