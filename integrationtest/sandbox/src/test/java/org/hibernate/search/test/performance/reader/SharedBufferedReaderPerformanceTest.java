/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.reader;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.indexes.impl.SharingBufferReaderProvider;

/**
 * @author Emmanuel Bernard
 */
public class SharedBufferedReaderPerformanceTest extends ReaderPerformanceTestCase {
	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.READER_STRATEGY, SharingBufferReaderProvider.class.getCanonicalName() );
	}
}
