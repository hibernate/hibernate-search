/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.test.file.FileUtils;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Benchmark)
public class TemporaryFileHolder {

	/**
	 * Set this system property to an alternative path if you want
	 * cache files (downloads) to be stored in a specific place
	 * (other than "/tmp/" + TEST_DIR_PREFIX + "cache-path").
	 */
	private static final String CACHE_PATH_PROPERTY_KEY = "cache-path";

	/**
	 * Set this system property to an alternative path if you want
	 * index files to be stored in a specific place
	 * (other than "/tmp/" + TEST_DIR_PREFIX + "indexes-path").
	 */
	private static final String INDEXES_PATH_PROPERTY_KEY = "indexes-path";

	/**
	 * Prefix used to identify the generated directories for
	 * running tests which need writing to a filesystem.
	 */
	private static final String TEST_DIR_PREFIX = "hsearch-perf-";

	private final Set<Path> toCleanUp = new LinkedHashSet<>();

	@TearDown(Level.Trial)
	public void cleanUp() throws IOException {
		deleteAll( toCleanUp );
	}

	public Path getCacheDirectory() throws IOException {
		return getTemporaryDirectory( CACHE_PATH_PROPERTY_KEY, false );
	}

	public Path getIndexesDirectory() throws IOException {
		return getTemporaryDirectory( INDEXES_PATH_PROPERTY_KEY, true );
	}

	private Path getTemporaryDirectory(String key, boolean cleanup) throws IOException {
		String userSelectedPath = System.getProperty( key );
		Path path;
		if ( userSelectedPath != null ) {
			path = Paths.get( userSelectedPath );
		}
		else {
			path = Paths.get( System.getProperty( "java.io.tmpdir" ) )
					.resolve( TEST_DIR_PREFIX + key );
		}

		if ( cleanup ) {
			if ( Files.exists( path ) ) {
				FileUtils.deleteRecursively( path );
			}
			toCleanUp.add( path );
		}

		if ( !Files.exists( path ) ) {
			Files.createDirectory( path );
		}

		return path;
	}

	private void deleteAll(Set<Path> paths) throws IOException {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.pushAll( FileUtils::deleteRecursively, paths );
		}
	}
}
