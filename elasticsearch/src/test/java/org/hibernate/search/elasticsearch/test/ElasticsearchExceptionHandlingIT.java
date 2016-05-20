/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.lucene.document.Document;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.elasticsearch.client.impl.BulkRequestFailedException;
import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Test;

/**
 * Tests invocation of the error handler during indexing. For that, a malicious field bridge is used which writes
 * unexpected fields for specific documents, causing these updates to fail.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchExceptionHandlingIT extends SearchTestBase {

	private static TestExceptionHandler errorHandler = new TestExceptionHandler();

	@After
	public void deleteTestDataAndResetErrorHandler() {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match_all' : {} } }" );
		List<?> result = session.createFullTextQuery( query ).list();

		for ( Object entity : result ) {
			session.delete( entity );
		}

		tx.commit();
		s.close();

		errorHandler.reset();
	}

	@Test
	public void errorHandlerInvokedForSingleOperation() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		Actor bert = new Actor();
		bert.id = 1L;
		bert.name = "Bert";
		s.persist( bert );

		tx.commit();

		assertThat( errorHandler.getHandleInvocations() ).hasSize( 1 );
		ErrorContext errorContext = errorHandler.getHandleInvocations().iterator().next();

		assertThat( errorContext.getThrowable() ).isExactlyInstanceOf( SearchException.class );
		assertThat( errorContext.getFailingOperations() ).onProperty( "idInString" ).containsOnly( "1" );

		tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match_all' : {} } }" );
		List<?> result = session.createFullTextQuery( query, Actor.class ).list();

		assertThat( result ).isEmpty();

		tx.commit();
		s.close();
	}

	@Test
	public void errorHandlerInvokedForBulk() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		Actor bob = new Actor();
		bob.id = 1L;
		bob.name = "Bob";
		s.persist( bob );

		Actor bruce = new Actor();
		bruce.id = 2L;
		bruce.name = "Bruce";
		s.persist( bruce );

		Actor bert = new Actor();
		bert.id = 3L;
		bert.name = "Bert";
		s.persist( bert );

		Actor brent = new Actor();
		brent.id = 4L;
		brent.name = "Brent";
		s.persist( brent );

		tx.commit();

		assertThat( errorHandler.getHandleInvocations() ).hasSize( 1 );
		ErrorContext errorContext = errorHandler.getHandleInvocations().iterator().next();

		assertThat( errorContext.getThrowable() ).isExactlyInstanceOf( BulkRequestFailedException.class );
		assertThat( errorContext.getFailingOperations() ).onProperty( "idInString" ).containsOnly( "3" );

		tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match_all' : {} } }" );
		List<?> result = session.createFullTextQuery( query, Actor.class ).list();

		assertThat( result ).onProperty( "name" ).containsOnly( "Bob", "Bruce", "Brent" );

		tx.commit();
		s.close();
	}

	@Override
	public void configure(Map<String, Object> settings) {
		settings.put( Environment.ERROR_HANDLER, errorHandler );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Actor.class };
	}

	public static class TestExceptionHandler implements ErrorHandler {

		private List<ErrorContext> handleInvocations = new ArrayList<>();

		@Override
		public void handle(ErrorContext context) {
			handleInvocations.add( context );
		}

		@Override
		public void handleException(String errorMsg, Throwable exception) {
		}

		public List<ErrorContext> getHandleInvocations() {
			return handleInvocations;
		}

		public void reset() {
			handleInvocations.clear();
		}
	}

	@Entity
	@Indexed(index = "actor")
	public static class Actor {

		@Id
		public Long id;

		@Field(bridge = @FieldBridge(impl = ErroneousFieldBridge.class))
		public String name;

		public String getName() {
			return name;
		}
	}

	public static class ErroneousFieldBridge implements org.hibernate.search.bridge.FieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			String asString = (String) value;
			luceneOptions.addFieldToDocument( name, asString, document );

			if ( "Bert".equals( asString ) ) {
				luceneOptions.addFieldToDocument( "unexpected", "unexpected", document );
			}
		}
	}
}
