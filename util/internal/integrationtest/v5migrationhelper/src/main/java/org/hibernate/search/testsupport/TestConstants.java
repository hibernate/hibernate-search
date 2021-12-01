/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

/**
 * Collects static constants used across several tests.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public final class TestConstants {

	public static final Analyzer standardAnalyzer = new StandardAnalyzer();
	public static final Analyzer stopAnalyzer = new StopAnalyzer( EnglishAnalyzer.ENGLISH_STOP_WORDS_SET );
	public static final Analyzer keywordAnalyzer = new KeywordAnalyzer();

	private TestConstants() {
		//not allowed
	}

	public static Version getTargetLuceneVersion() {
		return Version.LATEST;
	}

}
