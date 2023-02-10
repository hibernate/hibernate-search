/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.List;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.HighlightProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.HighlightProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.HighlightProjectionBuilder;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;

public class HighlightProjectionOptionsStepImpl implements HighlightProjectionOptionsStep,
		HighlightProjectionFinalStep {

	private final HighlightProjectionBuilder highlight;

	public HighlightProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext, String fieldPath) {
		this.highlight = dslContext.scope().fieldQueryElement( fieldPath, ProjectionTypeKeys.HIGHLIGHT );
	}

	@Override
	public HighlightProjectionFinalStep highlighter(String highlighterName) {
		highlight.highlighter( highlighterName );
		return this;
	}

	@Override
	public SearchProjection<List<String>> toProjection() {
		return highlight.build();
	}
}
