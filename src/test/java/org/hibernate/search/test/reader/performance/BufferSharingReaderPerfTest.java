// $Id$
package org.hibernate.search.test.reader.performance;

import org.hibernate.search.reader.SharingBufferReaderProvider;

/**
 * @author Sanne Grinovero
 */
public class BufferSharingReaderPerfTest extends ReaderPerformance {

	@Override
	protected String getReaderStrategyName() {
		return SharingBufferReaderProvider.class.getName();
	}

}
