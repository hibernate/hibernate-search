// $Id$
package org.hibernate.search.test.reader.performance;

/**
 * @author Sanne Grinovero
 */
public class SharedReaderPerfTest extends ReaderPerformance {

	@Override
	protected String getReaderStrategyName() {
		return "shared";
	}

}
