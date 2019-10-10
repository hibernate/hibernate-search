/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import java.nio.file.Paths;

import org.jboss.logging.Logger;

public class LuceneTestIndexesPathConfiguration {

	private static final Logger log = Logger.getLogger( LuceneTestIndexesPathConfiguration.class.getName() );

	private static LuceneTestIndexesPathConfiguration instance;

	public static LuceneTestIndexesPathConfiguration get() {
		if ( instance == null ) {
			instance = new LuceneTestIndexesPathConfiguration();
		}
		return instance;
	}

	private final String path;

	private LuceneTestIndexesPathConfiguration() {
		String pathFromSystemProperties = System.getProperty( "test.lucene.indexes.path" );
		if ( pathFromSystemProperties == null ) {
			// Happens when running tests from the IDE.
			// Assume tests are run from the module's root directory.
			this.path = Paths.get( "target/test-indexes" ).toAbsolutePath().toString();
		}
		else {
			this.path = pathFromSystemProperties;
		}

		log.infof(
				"Integration tests will put indexes in directory '%s'",
				path
		);
	}

	public String getPath() {
		return path;
	}
}
