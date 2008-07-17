// $Id$
package org.hibernate.search.test.reader.performance;

/**
 * @author Sanne Grinovero
 */
public class NotSharedReaderPerfTest extends ReaderPerformance {

	@Override
	protected String getReaderStrategyName() {
		return "not-shared";
	}

}
