/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

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
	 * Returns the target directory of the build.
	 *
	 * @param testClass the test class for which the target directory is requested.
	 * @return the target directory of the build
	 */
	public static Path getTargetDir(Class<?> testClass) {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		// get a URL reference to something we now is part of the classpath (our own classes)
		String currentTestClass = testClass.getName();
		int hopsToCompileDirectory = currentTestClass.split( "\\." ).length;
		int hopsToTargetDirectory = hopsToCompileDirectory + 1;
		URL classURL = contextClassLoader.getResource( currentTestClass.replace( '.', '/' ) + ".class" );
		// navigate back to '/target'
		Path targetDir;
		try {
			targetDir = Paths.get( classURL.toURI() );
		}
		catch (URISyntaxException e) {
			throw new RuntimeException( e );
		}
		// navigate back to '/target'
		for ( int i = 0; i < hopsToTargetDirectory; i++ ) {
			targetDir = targetDir.getParent();
		}
		return targetDir;
	}

	/**
	 * Return the root directory to store test indexes in. Tests should never use or delete this directly
	 * but rather nest sub directories in it to avoid interferences across tests.
	 *
	 * @param testClass the test class for which the index directory is requested.
	 * @return Return the root directory to store test indexes
	 */
	public static String getIndexDirectory(Class<?> testClass) {
		Path targetDir = getTargetDir( testClass );
		Path indexDirPath = targetDir.resolve( "indextemp" );
		String indexDir = indexDirPath.toAbsolutePath().toString();
		log.debugf( "Using %s as index directory.", indexDir );
		return indexDir;
	}

	public static boolean arePerformanceTestsEnabled() {
		return Boolean.getBoolean( "org.hibernate.search.enable_performance_tests" );
	}
}
