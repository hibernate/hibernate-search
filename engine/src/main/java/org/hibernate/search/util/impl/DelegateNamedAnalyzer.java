/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
