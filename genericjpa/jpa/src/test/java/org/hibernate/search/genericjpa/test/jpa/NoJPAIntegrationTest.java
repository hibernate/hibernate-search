/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.jpa;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.search.genericjpa.Constants;
import org.hibernate.search.genericjpa.Setup;
import org.hibernate.search.genericjpa.entity.EntityProvider;
import org.hibernate.search.genericjpa.impl.JPASearchFactoryAdapter;
import org.hibernate.search.genericjpa.test.jpa.entities.NonJPAEntity;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by Martin on 04.07.2015.
 */
public class NoJPAIntegrationTest {

	private JPASearchFactoryAdapter searchFactory;
	private FullTextEntityManager fem;

	@Before
	public void setup() {
		Properties properties = new Properties();
		properties.setProperty( "org.hibernate.search.genericjpa.searchfactory.name", "testCustomUpdatedEntity" );
		properties.setProperty( Constants.ADDITIONAL_INDEXED_TYPES_KEY, NonJPAEntity.class.getName() );
		properties.setProperty( "hibernate.search.searchfactory.type", "manual-updates" );
		this.searchFactory = (JPASearchFactoryAdapter) Setup.createSearchFactoryController( null, properties );
		this.fem = this.searchFactory.getFullTextEntityManager( null );
	}

	@Test
	public void test() {
		this.indexStuff();

		final NonJPAEntity tmp = new NonJPAEntity();

		NonJPAEntity fromQuery = (NonJPAEntity) this.fem.createFullTextQuery(
				new MatchAllDocsQuery(),
				NonJPAEntity.class
		)
				.initializeObjectsWith(
						ObjectLookupMethod.SKIP, DatabaseRetrievalMethod.FIND_BY_ID
				)
				.entityProvider(
						new EntityProvider() {
							@Override
							public Object get(Class<?> entityClass, Object id, Map<String, Object> hints) {
								return tmp;
							}

							@Override
							public List getBatch(Class<?> entityClass, List<Object> id, Map<String, Object> hints) {
								throw new AssertionError();
							}

							@Override
							public void close() throws IOException {
								//no-op
							}
						}
				)
				.getResultList()
				.get( 0 );

		assertEquals( tmp, fromQuery );
	}

	@Test
	public void testExpectedExceptions() {
		this.indexStuff();

		expectException( () -> this.fem.createFullTextQuery( new MatchAllDocsQuery() ).getResultList() );
		expectException( () -> this.fem.createFullTextQuery( new MatchAllDocsQuery() ).getSingleResult() );
	}

	private void indexStuff() {
		final NonJPAEntity tmp = new NonJPAEntity();
		tmp.setDocumentId( "toast" );
		this.fem.beginSearchTransaction();
		this.fem.index( tmp );
		this.fem.commitSearchTransaction();
	}

	private static void expectException(Runnable run) {
		try {
			run.run();
			fail("Exception expected!");
		}
		catch (Exception e) {
		}
	}

	@After
	public void shutdown() {
		this.searchFactory.close();
	}

}
