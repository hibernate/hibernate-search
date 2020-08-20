/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import org.apache.lucene.analysis.Analyzer;

/**
 * @author Yoann Rodiere
 */
public class NamedLuceneNormalizerReference extends NamedLuceneAnalyzerReference {

	public NamedLuceneNormalizerReference(String name) {
		super( name );
	}

	@Override
	public boolean isNormalizer(String fieldName) {
		return true;
	}

	@Override
	protected Analyzer createAnalyzer(LuceneAnalyzerBuilder builder) {
		return builder.buildNormalizer( getAnalyzerName() );
	}
}
