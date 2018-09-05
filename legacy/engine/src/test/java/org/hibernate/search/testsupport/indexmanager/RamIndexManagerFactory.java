/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.indexmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.WorkerBuildContextForTest;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;

/**
 * A {@link Rule} for use in JUnit tests where references to index managers are required.
 * <p>Use it this way:
 * <code><pre>
 * 	&commat;Rule
 * 	public RamIndexManagerFactory managerFactory = new RamIndexManagerFactory();
 *
 * 	&commat;Before
 * 	public void setUp() throws Exception {
 * 		IndexManager[] managers = managerFactory.createArray( 2 );
 * 		// Here, use the managers for setting up your environment
 * 	}
 * </pre></code>
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 * @author Yoann Rodiere
 */
public class RamIndexManagerFactory extends ExternalResource implements TestRule {

	private final List<IndexManager> managersToDestroy = new ArrayList<>();

	@Override
	protected void after() {
		for ( IndexManager manager : managersToDestroy ) {
			manager.destroy();
		}
	}

	public IndexManager[] createArray( int length ) {
		IndexManager[] result = new IndexManager[length];
		for ( int i = 0; i < length; ++i ) {
			result[i] = create();
		}
		return result;
	}

	public IndexManager create() {
		RamIndexManager ramIndexManager = new RamIndexManager();
		Properties properties = new Properties();
		properties.setProperty( "directory_provider", "local-heap" );
		ramIndexManager.initialize(
				"testIndex",
				properties,
				new ClassicSimilarity(),
				new WorkerBuildContextForTest( new SearchConfigurationForTest() )
		);
		managersToDestroy.add( ramIndexManager );
		return ramIndexManager;
	}

	private static final class RamIndexManager extends DirectoryBasedIndexManager {
	}
}
