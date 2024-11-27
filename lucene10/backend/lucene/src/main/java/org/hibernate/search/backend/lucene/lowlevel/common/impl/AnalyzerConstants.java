/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.common.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;

public final class AnalyzerConstants {

	public static final Analyzer KEYWORD_ANALYZER = new KeywordAnalyzer();

	private AnalyzerConstants() {
		// Not used
	}
}
