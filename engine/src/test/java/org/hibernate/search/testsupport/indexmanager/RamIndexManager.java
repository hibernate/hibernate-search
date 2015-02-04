/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.indexmanager;

import java.util.Properties;

import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.WorkerBuildContextForTest;

/**
 * At this point mainly used for tests
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class RamIndexManager extends DirectoryBasedIndexManager {

	public static RamIndexManager makeRamDirectory() {
		RamIndexManager ramIndexManager = new RamIndexManager();
		Properties properties = new Properties();
		properties.setProperty( "directory_provider", "ram" );
		ramIndexManager.initialize(
				"testIndex",
				properties,
				new DefaultSimilarity(),
				new WorkerBuildContextForTest( new SearchConfigurationForTest() )
		);
		return ramIndexManager;
	}
}
