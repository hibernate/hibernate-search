/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.impl;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A reference to a Lucene analyzer that also provides a name for this analyzer.
 * <p>
 * Such a reference may initially only contain the analyzer name, but in this case
 * it will be fully initialized with the actual analyzer later.
 *
 * @author Davide D'Alto
 */
public class NamedLuceneAnalyzerReference extends LuceneAnalyzerReference {

	private static final Log LOG = LoggerFactory.make();

	private String name;

	private Analyzer analyzer;

	public NamedLuceneAnalyzerReference(String name) {
		this.name = name;
		this.analyzer = null; // Not initialized yet
	}

	public String getAnalyzerName() {
		return name;
	}

	@Override
	public boolean isNormalizer(String fieldName) {
		return false;
	}

	@Override
	public Analyzer getAnalyzer() {
		if ( analyzer == null ) {
			throw LOG.lazyLuceneAnalyzerReferenceNotInitialized( this );
		}
		return analyzer;
	}

	public boolean isInitialized() {
		return analyzer != null;
	}

	public void initialize(LuceneAnalyzerBuilder builder) {
		if ( this.analyzer != null ) {
			throw new AssertionFailure( "A lucene analyzer reference has been initialized more than once: " + this );
		}
		this.analyzer = createAnalyzer(builder);
	}

	protected Analyzer createAnalyzer(LuceneAnalyzerBuilder builder) {
		return builder.buildAnalyzer( name );
	}

	@Override
	public void close() {
		if ( analyzer != null ) {
			analyzer.close();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		if ( analyzer != null ) {
			sb.append( analyzer );
		}
		else {
			sb.append( name );
		}
		sb.append( ">" );
		return sb.toString();
	}
}
