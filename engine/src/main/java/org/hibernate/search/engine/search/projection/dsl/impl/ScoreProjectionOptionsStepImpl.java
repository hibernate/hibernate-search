/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ScoreProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.spi.ScoreProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;


public class ScoreProjectionOptionsStepImpl implements ScoreProjectionOptionsStep {

	private final ScoreProjectionBuilder scoreProjectionBuilder;

	ScoreProjectionOptionsStepImpl(SearchProjectionBuilderFactory factory) {
		this.scoreProjectionBuilder = factory.score();
	}

	@Override
	public SearchProjection<Float> toProjection() {
		return scoreProjectionBuilder.build();
	}

}
