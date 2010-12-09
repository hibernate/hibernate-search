package org.hibernate.search.test.batchindexing;

import org.apache.lucene.store.LockObtainFailedException;
import org.slf4j.Logger;

import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.LogErrorHandler;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.util.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class DoNotCloseOnLockTimeoutTest extends SearchTestCase {

	private static Logger log = LoggerFactory.make();
	//On a 2008 MacBook Pro 2.4 GHz HD7200 t/min 1000 leads to the exception case (when not fixed)
	//putting 2000 to be safe on better machines
	public static final int NBR_OF_OBJECTS_TESTED = 2000;

	public void testTimeoutException() throws Exception {
		//reset log
		FailOnSomeExceptionErrorHandler.hasFailed = null;

		FullTextSession session = Search.getFullTextSession( openSession() );
		Transaction tx = session.beginTransaction();
		for ( int i = 0; i < NBR_OF_OBJECTS_TESTED; i++ ) {
			session.persist( new ConcurrentData( "concurrent data " + i ) );
			if ( i % 1000 == 0 ) {
				tx.commit();
				session.clear();
				tx = session.beginTransaction();
			}
		}
		tx.commit();
		session.clear();

		Thread massIndexer = new Thread( new MassindexerWork() );
		massIndexer.start();

		for ( int i = 1;  i < NBR_OF_OBJECTS_TESTED; i++ ) {
			tx = session.beginTransaction();
			ConcurrentData data = (ConcurrentData) session.get( ConcurrentData.class, new Long(i) );
			if ( i % 10 == 0 ) {
				log.debug("****** Editing " + i );
				data.setData( "This is a new data " + i );
				tx.commit();
				session.clear();
				tx = session.beginTransaction();
			}
			tx.commit();
		}
		session.clear();

		massIndexer.join();

		tx = session.beginTransaction();
		session.createQuery( "delete from " + ConcurrentData.class.getName() ).executeUpdate();
		tx.commit();
		session.close();

		if ( FailOnSomeExceptionErrorHandler.hasFailed != null) {
			FailOnSomeExceptionErrorHandler.hasFailed.printStackTrace(  );
			fail( "Unexpected exception while indexing:" + FailOnSomeExceptionErrorHandler.hasFailed.getMessage() );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ConcurrentData.class
		};
	}

	private class MassindexerWork implements Runnable {
		public void run() {
			FullTextSession session = Search.getFullTextSession( openSession() );
			try {
				session.createIndexer( ConcurrentData.class ).startAndWait();
				session.createIndexer( ConcurrentData.class ).startAndWait();
			}
			catch ( InterruptedException e ) {
				log.error( "Error while reindexing", e );
			}
			finally {
				session.close();
			}
		}
	}

	public static class FailOnSomeExceptionErrorHandler extends LogErrorHandler implements ErrorHandler {

		public static volatile Throwable hasFailed;

		public FailOnSomeExceptionErrorHandler() {}

		public void handle(ErrorContext context) {
			if ( hasFailed == null && context.getThrowable() != null && ! ( context.getThrowable().getCause() instanceof LockObtainFailedException ) ) {
				hasFailed = context.getThrowable();
				super.handle( context );
			}
			if ( context.getThrowable() != null && ! ( context.getThrowable().getCause() instanceof LockObtainFailedException ) ) {
				super.handle( context );
			}
			//super.handle( context );
		}
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ERROR_HANDLER, FailOnSomeExceptionErrorHandler.class.getName() );
	}
}
