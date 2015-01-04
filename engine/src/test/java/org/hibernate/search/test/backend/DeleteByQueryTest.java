/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.ClassicQueryParserQuery;
import org.hibernate.search.backend.CustomBehaviour;
import org.hibernate.search.backend.CustomBehaviourQuery;
import org.hibernate.search.backend.DeleteByQuerySupport;
import org.hibernate.search.backend.DeletionQuery;
import org.hibernate.search.backend.SingularTermQuery;
import org.hibernate.search.backend.spi.DeleteByQueryWork;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.hibernate.search.util.impl.ScopedAnalyzer;
import org.junit.Rule;
import org.junit.Test;

public class DeleteByQueryTest {

	@Rule
	public SearchFactoryHolder factoryHolder = new SearchFactoryHolder( Book.class );

	@Test
	public void testWithWorker() {
		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();
		Worker worker = integrator.getWorker();

		{
			TransactionContextForTest tc = new TransactionContextForTest();
			for ( Work work : this.makeBooks() ) {
				worker.performWork( work, tc );
			}
			tc.end();
		}
		this.assertCount( 2, integrator );

		{
			TransactionContextForTest tc = new TransactionContextForTest();
			worker.performWork( new DeleteByQueryWork( Book.class, new SingularTermQuery( "id", String.valueOf( 5 ) ) ), tc );
			tc.end();
		}
		this.assertCount( 1, integrator );

		{
			CustomBehaviourQuery query = new CustomBehaviourQuery( MyCustomBehaviour.class, "6" );
			TransactionContextForTest tc = new TransactionContextForTest();
			worker.performWork( new DeleteByQueryWork( Book.class, query ), tc );
			tc.end();

			{
				String[] strRep = DeleteByQuerySupport.TO_STRING.get( query.getQueryKey() ).toString( query );
				DeletionQuery fromStrRep = DeleteByQuerySupport.FROM_STRING.get( query.getQueryKey() ).fromString( strRep );

				assertEquals( query, fromStrRep );
			}
		}
		this.assertCount( 0, integrator );

		// REINITIALIZE FOR FURTHER TESTS
		{
			TransactionContextForTest tc = new TransactionContextForTest();
			for ( Work work : this.makeBooks() ) {
				worker.performWork( work, tc );
			}
			tc.end();
		}
		this.assertCount( 2, integrator );

		{
			ClassicQueryParserQuery query = new ClassicQueryParserQuery( Version.LUCENE_4_10_2, "+id:5" );
			TransactionContextForTest tc = new TransactionContextForTest();
			worker.performWork( new DeleteByQueryWork( Book.class, query ), tc );
			tc.end();

			{
				String[] strRep = DeleteByQuerySupport.TO_STRING.get( query.getQueryKey() ).toString( query );
				DeletionQuery fromStrRep = DeleteByQuerySupport.FROM_STRING.get( query.getQueryKey() ).fromString( strRep );

				assertEquals( query, fromStrRep );
			}
		}
		this.assertCount( 1, integrator );
	}

	public static class MyCustomBehaviour implements CustomBehaviour {

		/*
		 * (non-Javadoc)
		 * @see
		 * org.hibernate.search.backend.CustomBehaviour#toLuceneQuery(org.hibernate.search.backend.CustomBehaviourQuery,
		 * org.hibernate.search.util.impl.ScopedAnalyzer)
		 */
		@Override
		public Query dataToLuceneQuery(Object data, ScopedAnalyzer analyzerForEntity) {
			String id = data.toString();
			return new TermQuery( new Term( "id", id ) );
		}

		/*
		 * (non-Javadoc)
		 * @see org.hibernate.search.backend.CustomBehaviour#toString(org.hibernate.search.backend.CustomBehaviourQuery)
		 */
		@Override
		public String[] dataToString(Object data) {
			return new String[] { data.toString() };
		}

		/*
		 * (non-Javadoc)
		 * @see org.hibernate.search.backend.CustomBehaviour#fromString(java.lang.String[])
		 */
		@Override
		public Object stringToData(String[] string) {
			return string[0];
		}

	}

	private void assertCount(int count, ExtendedSearchIntegrator integrator) {
		{
			Query query = integrator.buildQueryBuilder().forEntity( Book.class ).get().all().createQuery();
			HSQuery hsQuery = integrator.createHSQuery().luceneQuery( query );
			List<Class<?>> l = new ArrayList<>();
			l.add( Book.class );
			hsQuery.targetedEntities( l );
			assertEquals( count, hsQuery.queryResultSize() );
		}
	}

	private List<Work> makeBooks() {
		List<Work> list = new LinkedList<>();
		// just some random data:
		list.add( new Work( new Book( String.valueOf( 5 ), "Lord of The Rings" ), WorkType.ADD ) );
		list.add( new Work( new Book( String.valueOf( 6 ), "The Hobbit" ), WorkType.ADD ) );
		return list;
	}

	@Indexed(index = "books")
	private static class Book {

		@DocumentId
		String id;
		@Field
		String title;

		public Book(String id, String title) {
			super();
			this.id = id;
			this.title = title;
		}

	}

}
