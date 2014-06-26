/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.reader;

import org.hibernate.search.testsupport.TestConstants;

/**
 * @author Sanne Grinovero
 */
public class NotSharedReaderPerformanceTest extends ReaderPerformance {

	@Override
	protected String getReaderStrategyName() {
		return "not-shared";
	}

	@Override
	protected String getIndexBaseDir() {
		return TestConstants.getIndexDirectory( NotSharedReaderPerformanceTest.class ) + "NotSharedReaderPerformanceTest";
	}

}
