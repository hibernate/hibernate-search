/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Collects static constants used across several tests.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public final class TestConstants {

	public static final Analyzer standardAnalyzer = new StandardAnalyzer();
	public static final Analyzer stopAnalyzer = new StopAnalyzer();
	public static final Analyzer simpleAnalyzer = new SimpleAnalyzer();
	public static final Analyzer keywordAnalyzer = new KeywordAnalyzer();

	private static final Log log = LoggerFactory.make();

	private TestConstants() {
		//not allowed
	}

	public static Version getTargetLuceneVersion() {
		return Version.LATEST;
	}

	/**
	 * Returns a temporary directory for storing test data such as indexes etc. Specific tests should store their data
	 * in sub-directories. The returned directory will be deleted on graceful shut-down. The returned directory will be
	 * named like {@code $TEMP/hsearch-tests-<random>}.
	 */
	public static Path getTempTestDataDir() {
		try {
			Path tempTestDataDir = Files.createTempDirectory( "hsearch-tests-" );
			tempTestDataDir.toFile().deleteOnExit();

			return tempTestDataDir;
		}
		catch (IOException e) {
			throw new RuntimeException( "Could not create temporary directory for tests", e );
		}
	}

	/**
	 * Return the root directory to store test indexes in. Tests should never use or delete this directly
	 * but rather nest sub directories in it to avoid interferences across tests.
	 *
	 * @return Return the root directory to store test indexes
	 */
	public static String getIndexDirectory(Path parent) {
		Path indexDirPath = parent.resolve( "indextemp" );
		indexDirPath.toFile().deleteOnExit();

		String indexDir = indexDirPath.toAbsolutePath().toString();
		log.debugf( "Using %s as index directory.", indexDir );
		return indexDir;
	}

	public static boolean arePerformanceTestsEnabled() {
		return Boolean.getBoolean( "org.hibernate.search.enable_performance_tests" );
	}
}
