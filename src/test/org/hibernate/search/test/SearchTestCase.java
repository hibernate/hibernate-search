//$Id$
package org.hibernate.search.test;

import java.io.File;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.store.Directory;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.test.inheritance.Animal;
import org.hibernate.search.test.inheritance.Mammal;
import org.hibernate.search.event.FullTextIndexEventListener;
import org.hibernate.search.store.RAMDirectoryProvider;
import org.hibernate.search.store.FSDirectoryProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for Hibernate Search unit tests.
 * 
 * @author Emmanuel Bernard
 */
public abstract class SearchTestCase extends HANTestCase {
	
	private static final Logger log = LoggerFactory
			.getLogger(SearchTestCase.class);
	
	private static File indexDir;
	static {
		String buildDir = System.getProperty("build.dir");
		if (buildDir == null) {
			buildDir = ".";
		}
		File current = new File( buildDir );
		indexDir = new File( current, "indextemp" );
		log.debug("Using {} as index directory.", indexDir.getAbsolutePath());
	}
	
	protected void setUp() throws Exception {
		buildSessionFactory( getMappings(), getAnnotatedPackages(), getXmlFiles() );
	}

	@SuppressWarnings("unchecked")
	protected Directory getDirectory(Class clazz) {
		return getLuceneEventListener().getSearchFactoryImplementor().getDirectoryProviders( clazz )[0].getDirectory();
	}

	private FullTextIndexEventListener getLuceneEventListener() {
        PostInsertEventListener[] listeners = ( (SessionFactoryImpl) getSessions() ).getEventListeners().getPostInsertEventListeners();
        FullTextIndexEventListener listener = null;
        //FIXME this sucks since we mandante the event listener use
        for (PostInsertEventListener candidate : listeners) {
            if (candidate instanceof FullTextIndexEventListener ) {
                listener = (FullTextIndexEventListener) candidate;
                break;
            }
        }
        if (listener == null) throw new HibernateException("Lucene event listener not initialized");
        return listener;
    }

	protected void ensureIndexesAreEmpty() {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx;
		tx = s.beginTransaction();
		for ( Class clazz : getMappings() ) {
			if ( clazz.getAnnotation( Indexed.class ) != null ) {
				s.purgeAll( clazz );
			}
		}
		tx.commit();
	}

	protected void configure(org.hibernate.cfg.Configuration cfg) {
		cfg.setProperty( "hibernate.search.default.directory_provider", RAMDirectoryProvider.class.getName() );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		cfg.setProperty( "hibernate.search.default.transaction.merge_factor", "100" );
		cfg.setProperty( "hibernate.search.default.batch.max_buffered_docs", "1000" );
	}
	
	protected File getBaseIndexDir() {
		return indexDir;
	}
}
