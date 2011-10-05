/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test;

import java.io.File;
import java.net.URL;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Collects static constants used across several tests.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class TestConstants {

	private static final Log log = LoggerFactory.make();

	public static final Analyzer standardAnalyzer = new StandardAnalyzer( TestConstants.getTargetLuceneVersion() );
	public static final Analyzer stopAnalyzer = new StopAnalyzer( TestConstants.getTargetLuceneVersion() );
	public static final Analyzer simpleAnalyzer = new SimpleAnalyzer( TestConstants.getTargetLuceneVersion() );
	public static final Analyzer keywordAnalyzer = new KeywordAnalyzer();

	private static File targetDir;
	static final File indexDir;

	static {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		// get a URL reference to something we now is part of the classpath (us)
		URL myUrl = contextClassLoader.getResource( TestConstants.class.getName().replace( '.', '/' ) + ".class" );
		File myPath = new File( myUrl.getFile() );
		// navigate back to '/target'
		targetDir = myPath
				.getParentFile()  // target/classes/org/hibernate/search/test
				.getParentFile()  // target/classes/org/hibernate/search
				.getParentFile()  // target/classes/org/hibernate/
				.getParentFile()  // target/classes/org
				.getParentFile()  // target/classes/
				.getParentFile(); // target

		indexDir = new File( targetDir, "indextemp" );
		log.debugf( "Using %s as index directory.", indexDir.getAbsolutePath() );
	}

	public static Version getTargetLuceneVersion() {
		return Version.LUCENE_CURRENT;
	}

	/**
	 * Returns the target directory of the build.
	 *
	 * @return the target directory of the build
	 */
	public static File getTargetDir() {
		return targetDir;
	}

}
