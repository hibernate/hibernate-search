/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.analysis.impl;

import java.util.Objects;

import org.hibernate.search.engine.backend.analysis.AnalyzerDescriptor;
import org.hibernate.search.engine.backend.analysis.NormalizerDescriptor;

public class ElasticsearchAnalysisDescriptor
		implements AnalyzerDescriptor,
		NormalizerDescriptor {


	private final String name;

	public ElasticsearchAnalysisDescriptor(String name) {
		this.name = name;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		if ( object == null || getClass() != object.getClass() ) {
			return false;
		}
		ElasticsearchAnalysisDescriptor that = (ElasticsearchAnalysisDescriptor) object;
		return Objects.equals( name, that.name );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name );
	}
}
