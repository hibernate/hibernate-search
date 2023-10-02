/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.impl;

import java.util.Objects;

import org.hibernate.search.engine.backend.analysis.AnalyzerDescriptor;
import org.hibernate.search.engine.backend.analysis.NormalizerDescriptor;

import org.apache.lucene.analysis.Analyzer;

public class LuceneAnalysisDescriptor implements AnalyzerDescriptor, NormalizerDescriptor {

	protected final String name;
	protected final Analyzer analyzer;

	public LuceneAnalysisDescriptor(String name, Analyzer analyzer) {
		this.name = name;
		this.analyzer = analyzer;
	}

	@Override
	public String name() {
		return name;
	}

	public Analyzer analyzer() {
		return analyzer;
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		if ( object == null || getClass() != object.getClass() ) {
			return false;
		}
		LuceneAnalysisDescriptor that = (LuceneAnalysisDescriptor) object;
		return Objects.equals( name, that.name ) && Objects.equals( analyzer, that.analyzer );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, analyzer );
	}

}
