//$Id$
package org.hibernate.search.test.reader;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.reader.SharedReaderProvider;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings("deprecation")
public class SharedReaderPerfTest extends ReaderPerfTestCase {
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.READER_STRATEGY, SharedReaderProvider.class.getCanonicalName() );
	}
}
