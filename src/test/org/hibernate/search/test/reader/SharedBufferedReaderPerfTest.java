//$Id$
package org.hibernate.search.test.reader;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.reader.SharingBufferReaderProvider;

/**
 * @author Emmanuel Bernard
 */
public class SharedBufferedReaderPerfTest extends ReaderPerfTestCase {
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.READER_STRATEGY, SharingBufferReaderProvider.class.getCanonicalName() );
	}
}