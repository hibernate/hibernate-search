/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.factory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.genericjpa.entity.EntityProvider;
import org.hibernate.search.genericjpa.factory.StandaloneSearchConfiguration;
import org.hibernate.search.genericjpa.factory.StandaloneSearchFactory;
import org.hibernate.search.genericjpa.factory.StandaloneSearchFactoryFactory;
import org.hibernate.search.genericjpa.factory.Transaction;
import org.hibernate.search.genericjpa.query.HSearchQuery;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SearchFactoryTest {

	@Test
	public void testWithoutNewClasses() {
		SearchConfiguration searchConfiguration = new StandaloneSearchConfiguration();
		List<Class<?>> classes = Arrays.asList( TopLevel.class );

		SearchIntegratorBuilder builder = new SearchIntegratorBuilder();
		// we have to build an integrator here (but we don't need it afterwards)
		builder.configuration( searchConfiguration ).buildSearchIntegrator();
		classes.forEach(
				(clazz) -> {
					builder.addClass( clazz );
				}
		);
		SearchIntegrator impl = builder.buildSearchIntegrator();

		TopLevel tl = new TopLevel();
		tl.setId( 123 );
		Embedded eb = new Embedded( 1 );

		List<Embedded2> embedded2 = new ArrayList<>();
		{
			Embedded2 e1 = new Embedded2();
			e1.setEmbedded( eb );
			embedded2.add( e1 );

			Embedded2 e2 = new Embedded2();
			e2.setEmbedded( eb );
			embedded2.add( e1 );
		}
		eb.setEmbedded2( embedded2 );

		tl.setEmbedded( eb );
		Transaction tc = new Transaction();

		impl.getWorker().performWork( new Work( tl, WorkType.ADD ), tc );

		tc.commit();

		assertEquals(
				1, impl.createHSQuery().luceneQuery( new MatchAllDocsQuery() ).targetedEntities(
						Collections.singletonList(
								TopLevel.class
						)
				).queryResultSize()
		);
	}

	@Test
	public void test() throws IOException {
		StandaloneSearchFactory factory = StandaloneSearchFactoryFactory.createSearchFactory(
				new StandaloneSearchConfiguration(),
				Arrays.asList( TopLevel.class, Embedded.class, Embedded2.class )
		);
		try {

			//both have the same id
			//this is important for the multi-entity query
			TopLevel tl = new TopLevel();
			tl.setId( 1 );
			Embedded eb = new Embedded( 1 );

			List<Embedded2> embedded2 = new ArrayList<>();
			{
				Embedded2 e1 = new Embedded2();
				e1.setEmbedded( eb );
				embedded2.add( e1 );

				Embedded2 e2 = new Embedded2();
				e2.setEmbedded( eb );
				embedded2.add( e1 );
			}
			eb.setEmbedded2( embedded2 );

			tl.setEmbedded( eb );
			eb.setTopLevel( tl );

			// indexing starting from the contained entity should work as well
			// :)
			factory.index( embedded2.get( 0 ) );

			assertEquals( 1, factory.createQuery( new MatchAllDocsQuery(), TopLevel.class ).queryResultSize() );
			assertEquals( 1, factory.createQuery( new MatchAllDocsQuery(), Embedded.class ).queryResultSize() );

			//we should find both in the index
			assertEquals(
					2, factory.createQuery( new MatchAllDocsQuery(), TopLevel.class, Embedded.class )
							.queryResultSize()
			);

			{
				final EntityProvider emptyProvider = new EntityProvider() {
					@Override
					public Object get(Class<?> entityClass, Object id, Map<String, Object> hints) {
						return null;
					}

					@Override
					public List getBatch(Class<?> entityClass, List<Object> id, Map<String, Object> hints) {
						return Collections.emptyList();
					}

					@Override
					public void close() throws IOException {

					}
				};

				//we shouldn't find anything for null/empty
				assertEquals(
						0, factory.createQuery( new MatchAllDocsQuery(), TopLevel.class, Embedded.class ).query(
								emptyProvider, HSearchQuery.Fetch.BATCH
						).size()
				);
				assertEquals(
						0, factory.createQuery( new MatchAllDocsQuery(), TopLevel.class, Embedded.class ).query(
								emptyProvider, HSearchQuery.Fetch.FIND_BY_ID
						).size()
				);
			}

			{
				final EntityProvider dummyProvider = new EntityProvider() {

					@Override
					public Object get(Class<?> entityClass, Object id, Map<String, Object> hints) {
						if ( TopLevel.class.equals( entityClass ) ) {
							TopLevel ret = new TopLevel();
							ret.setId( 1 );
							return ret;
						}
						else {
							Embedded ret = new Embedded( 1 );
							return ret;
						}
					}

					@Override
					public List getBatch(Class<?> entityClass, List<Object> id, Map<String, Object> hints) {
						return Collections.singletonList( this.get( entityClass, id.get( 0 ) ) );
					}

					@Override
					public void close() throws IOException {

					}
				};

				//we should find everything with the dummies
				assertEquals(
						2, factory.createQuery( new MatchAllDocsQuery(), TopLevel.class, Embedded.class ).query(
								dummyProvider, HSearchQuery.Fetch.BATCH
						).size()
				);
				assertEquals(
						2, factory.createQuery( new MatchAllDocsQuery(), TopLevel.class, Embedded.class ).query(
								dummyProvider, HSearchQuery.Fetch.FIND_BY_ID
						).size()
				);
			}

			//check if hints are propagated
			{
				final EntityProvider dummyProvider = new EntityProvider() {

					@Override
					public Object get(Class<?> entityClass, Object id, Map<String, Object> hints) {
						assertTrue( hints.size() > 0 );
						if ( TopLevel.class.equals( entityClass ) ) {
							TopLevel ret = new TopLevel();
							ret.setId( 1 );
							return ret;
						}
						else {
							Embedded ret = new Embedded( 1 );
							return ret;
						}
					}

					@Override
					public List getBatch(Class<?> entityClass, List<Object> id, Map<String, Object> hints) {
						return Collections.singletonList( this.get( entityClass, id.get( 0 ), hints ) );
					}

					@Override
					public void close() throws IOException {

					}
				};

				Map<String, Object> hints = new HashMap<>();
				hints.put( "1", 1 );

				//we should find everything with the dummies
				assertEquals(
						2, factory.createQuery( new MatchAllDocsQuery(), TopLevel.class, Embedded.class )
								.hints( hints )
								.query(
										dummyProvider, HSearchQuery.Fetch.BATCH
								)
								.size()
				);
				assertEquals(
						2, factory.createQuery( new MatchAllDocsQuery(), TopLevel.class, Embedded.class )
								.hints( hints )
								.query(
										dummyProvider, HSearchQuery.Fetch.FIND_BY_ID
								)
								.size()
				);
			}

			//test timeout
			{
				HSearchQuery query = factory.createQuery( new MatchAllDocsQuery(), TopLevel.class );
				query.setTimeout( 1, TimeUnit.NANOSECONDS );
				try {
					query.queryResultSize();
					fail( "Exception expected!" );
				}
				catch (Exception e) {
				}
			}

			//test limit fetch time
			{
				HSearchQuery query = factory.createQuery( new MatchAllDocsQuery(), TopLevel.class );
				query.limitExecutionTimeTo( 1, TimeUnit.NANOSECONDS );
				assertEquals( 0, query.queryResultSize() );
			}

			factory.purge( TopLevel.class, new TermQuery( new Term( "id", "1" ) ) );
			HSearchQuery query = factory.createQuery(
					factory.buildQueryBuilder()
							.forEntity( TopLevel.class )
							.get()
							.all()
							.createQuery(), TopLevel.class
			);
			assertEquals( 0, query.queryResultSize() );
		}
		finally {
			factory.close();
		}
	}

	@Test
	public void testNullInIndexNotReturned() throws IOException {
		StandaloneSearchFactory factory = StandaloneSearchFactoryFactory.createSearchFactory(
				new StandaloneSearchConfiguration(),
				Arrays.asList( TopLevel.class, Embedded.class, Embedded2.class )
		);
		try {
			//both have the same id
			//this is important for the multi-entity query
			TopLevel tl = new TopLevel();
			tl.setId( null );
			Embedded eb = new Embedded( null );

			tl.setEmbedded( eb );
			eb.setTopLevel( tl );

			//this indexes both
			factory.index( eb );

			final EntityProvider dummyProvider = new EntityProvider() {

				@Override
				public Object get(Class<?> entityClass, Object id, Map<String, Object> hints) {
					throw new AssertionError( "shoudn't try to load anything if the ids are null in the index" );
				}

				@Override
				public List getBatch(Class<?> entityClass, List<Object> id, Map<String, Object> hints) {
					throw new AssertionError( "shoudn't try to load anything if the ids are null in the index" );
				}

				@Override
				public void close() throws IOException {

				}
			};

			//we shouldn't find anything as the ids are null
			assertEquals(
					0, factory.createQuery( new MatchAllDocsQuery(), TopLevel.class, Embedded.class ).query(
							dummyProvider, HSearchQuery.Fetch.BATCH
					).size()
			);
			assertEquals(
					0, factory.createQuery( new MatchAllDocsQuery(), TopLevel.class, Embedded.class ).query(
							dummyProvider, HSearchQuery.Fetch.FIND_BY_ID
					).size()
			);
		}
		finally {
			factory.close();
		}
	}

	@Indexed
	public static class TopLevel {

		private Integer id;
		private Embedded embedded;

		@DocumentId
		public Integer getId() {
			return this.id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@IndexedEmbedded
		public Embedded getEmbedded() {
			return embedded;
		}

		public void setEmbedded(Embedded embedded) {
			this.embedded = embedded;
		}

	}

	@Indexed
	public static class Embedded {

		private final Integer id;

		public Embedded(Integer id) {
			this.id = id;
		}

		private String test;
		private TopLevel topLevel;
		private List<Embedded2> embedded2;

		@DocumentId
		public Integer getId() {
			return this.id;
		}

		@Field(store = Store.YES)
		public String getTest() {
			return this.test;
		}

		public void setTest(String test) {
			this.test = test;
		}

		@ContainedIn
		public TopLevel getTopLevel() {
			return this.topLevel;
		}

		public void setTopLevel(TopLevel topLevel) {
			this.topLevel = topLevel;
		}

		@IndexedEmbedded
		public List<Embedded2> getEmbedded2() {
			return embedded2;
		}

		public void setEmbedded2(List<Embedded2> embedded2) {
			this.embedded2 = embedded2;
		}

	}

	public static class Embedded2 {

		private String test;
		private Embedded embedded;

		@Field(store = Store.YES)
		public String getTest() {
			return this.test;
		}

		public void setTest(String test) {
			this.test = test;
		}

		@ContainedIn
		public Embedded getEmbedded() {
			return embedded;
		}

		public void setEmbedded(Embedded embedded) {
			this.embedded = embedded;
		}

	}
}
