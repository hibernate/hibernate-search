// $Id$
package org.hibernate.search.util;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

/**
 * delegate to a named analyzer
 * delegated Analyzers are lazily configured
 *
 * @author Emmanuel Bernard
 */
public class DelegateNamedAnalyzer extends Analyzer {
	private String name;
	private Analyzer delegate;

	public DelegateNamedAnalyzer(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setDelegate(Analyzer delegate) {
		this.delegate = delegate;
		this.name = null; //unique init
	}

	public TokenStream tokenStream(String fieldName, Reader reader) {
		return delegate.tokenStream( fieldName, reader );
	}

	@Override
	public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
		return delegate.reusableTokenStream( fieldName, reader );
	}

	@Override
	public int getPositionIncrementGap(String fieldName) {
		return delegate.getPositionIncrementGap( fieldName );
	}
}
