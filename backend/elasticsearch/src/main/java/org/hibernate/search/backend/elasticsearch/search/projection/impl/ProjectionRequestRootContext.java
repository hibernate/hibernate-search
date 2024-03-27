/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.search.highlighter.impl.ElasticsearchSearchHighlighter;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.spatial.GeoPoint;

public interface ProjectionRequestRootContext extends ProjectionRequestContext {

	Integer getDistanceSortIndex(String absoluteFieldPath, GeoPoint location);

	ElasticsearchSearchSyntax getSearchSyntax();

	ElasticsearchSearchHighlighter highlighter(String highlighterName);

	ElasticsearchSearchHighlighter queryHighlighter();

	boolean isCompatibleHighlighter(String highlighterName, ProjectionAccumulator.Provider<?, ?> accumulatorProvider);
}
