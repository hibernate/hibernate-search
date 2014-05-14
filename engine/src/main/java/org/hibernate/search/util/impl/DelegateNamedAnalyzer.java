/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;

/**
 * Delegate to a named analyzer. Delegated Analyzers are lazily configured.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public final class DelegateNamedAnalyzer extends AnalyzerWrapper {

	private String name;
	private Analyzer delegate;

	public DelegateNamedAnalyzer(String name) {
		super( Analyzer.GLOBAL_REUSE_STRATEGY );
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setDelegate(Analyzer delegate) {
		this.delegate = delegate;
		this.name = null; //unique init
	}

	@Override
	public void close() {
		super.close();
		delegate.close();
	}

	@Override
	protected Analyzer getWrappedAnalyzer(String fieldName) {
		return delegate;
	}

}
