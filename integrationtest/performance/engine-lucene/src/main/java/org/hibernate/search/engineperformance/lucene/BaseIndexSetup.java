/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.lucene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.hibernate.search.util.impl.FileHelper;

class BaseIndexSetup {

	/**
	 * Set this system property to an alternative path if you don't
	 * want the filesystem based performance tests to be run on your
	 * default temp path.
	 */
	private static final String INDEX_PATH_PROPERTY = "index-path";

	/**
	 * Prefix used to identify the generated temporary directories for
	 * running tests which need writing to a filesystem.
	 */
	private static final String TEST_DIR_PREFIX = "HibernateSearch-Perftests-";

	//Instance variable to allow cleanup after benchmark execution
	private Path createdTempDirectory;

	protected Path pickIndexStorageDirectory() throws IOException {
		String userSelectedPath = System.getProperty( INDEX_PATH_PROPERTY );
		if ( userSelectedPath != null ) {
			Path pathPrefix = Paths.get( userSelectedPath );
			createdTempDirectory = Files.createTempDirectory( pathPrefix, TEST_DIR_PREFIX );
		}
		else {
			createdTempDirectory = Files.createTempDirectory( TEST_DIR_PREFIX );
		}
		return createdTempDirectory;
	}

	protected void cleanup() throws IOException {
		FileHelper.tryDelete( createdTempDirectory );
	}

}
