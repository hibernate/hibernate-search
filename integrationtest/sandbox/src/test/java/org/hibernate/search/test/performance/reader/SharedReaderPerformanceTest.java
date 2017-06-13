/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.reader;

import java.nio.file.Path;

import org.hibernate.search.test.util.TargetDirHelper;
import org.hibernate.search.testsupport.TestConstants;

/**
 * @author Sanne Grinovero
 */
public class SharedReaderPerformanceTest extends ReaderPerformance {

	@Override
	protected String getReaderStrategyName() {
		return "shared";
	}

	@Override
	protected Path getIndexBaseDir() {
		return TestConstants.getIndexDirectory( TargetDirHelper.getTargetDir() );
	}

}
