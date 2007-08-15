//$Id$
package org.hibernate.search.test;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.store.Directory;
import org.hibernate.HibernateException;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.search.Environment;
import org.hibernate.search.event.FullTextIndexEventListener;
import org.hibernate.search.store.RAMDirectoryProvider;

/**
 * @author Emmanuel Bernard
 */
public abstract class SearchTestCase extends HANTestCase {
	protected void setUp() throws Exception {
		//super.setUp(); //we need a fresh session factory each time for index set up
		buildSessionFactory( getMappings(), getAnnotatedPackages(), getXmlFiles() );
	}

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

	protected void configure(org.hibernate.cfg.Configuration cfg) {
		cfg.setProperty( "hibernate.search.default.directory_provider", RAMDirectoryProvider.class.getName() );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		cfg.setProperty( "hibernate.search.default.transaction.merge_factor", "100" );
		cfg.setProperty( "hibernate.search.default.batch.max_buffered_docs", "1000" );
	}
}
