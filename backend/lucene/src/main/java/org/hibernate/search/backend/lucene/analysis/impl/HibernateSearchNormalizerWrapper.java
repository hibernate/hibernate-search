/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;

public final class HibernateSearchNormalizerWrapper extends AnalyzerWrapper {

	private final String normalizerName;

	private final Analyzer delegate;

	HibernateSearchNormalizerWrapper(String normalizerName, Analyzer delegate) {
		super( delegate.getReuseStrategy() );
		this.normalizerName = normalizerName;
		this.delegate = delegate;
	}

	@Override
	protected Analyzer getWrappedAnalyzer(String fieldName) {
		return delegate;
	}

	@Override
	protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
		HibernateSearchNormalizerCheckingFilter filter =
				new HibernateSearchNormalizerCheckingFilter( components.getTokenStream(), normalizerName );
		return new TokenStreamComponents( components.getSource(), filter );
	}

	@Override
	public String toString() {
		return "HibernateSearchNormalizerWrapper(" + delegate.toString() + ", normalizerName=" + normalizerName + ")";
	}

}
