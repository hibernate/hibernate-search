//$Id$
package org.hibernate.search.test.reader;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.store.FSDirectoryProvider;
import org.hibernate.search.Environment;
import org.apache.lucene.analysis.StopAnalyzer;

/**
 * @author Emmanuel Bernard
 */
public class NotSharedReaderPerfTest extends ReaderPerfTestCase {
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.directory_provider", FSDirectoryProvider.class.getName() );
		cfg.setProperty( "hibernate.search.default.indexBase", "./indextemp" );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		cfg.setProperty( Environment.READER_STRATEGY, "not-shared" );
	}
}
