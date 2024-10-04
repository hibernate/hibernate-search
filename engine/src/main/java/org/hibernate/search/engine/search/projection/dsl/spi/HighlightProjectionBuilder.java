/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.spi;

import org.hibernate.search.engine.search.projection.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.SearchProjection;

public abstract class HighlightProjectionBuilder {
	protected final String path;
	protected String highlighterName;

	protected HighlightProjectionBuilder(String path) {
		this.path = path;
	}

	public HighlightProjectionBuilder highlighter(String highlighterName) {
		this.highlighterName = highlighterName;
		return this;
	}

	public abstract <V> SearchProjection<V> build(ProjectionAccumulator.Provider<String, V> accumulatorProvider);
}
