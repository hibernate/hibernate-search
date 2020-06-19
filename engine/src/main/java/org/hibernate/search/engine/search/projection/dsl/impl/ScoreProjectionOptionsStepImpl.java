/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ScoreProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.ScoreProjectionBuilder;


public class ScoreProjectionOptionsStepImpl
		implements ScoreProjectionOptionsStep<ScoreProjectionOptionsStepImpl> {

	private final ScoreProjectionBuilder scoreProjectionBuilder;

	ScoreProjectionOptionsStepImpl(SearchProjectionDslContext<?> dslContext) {
		this.scoreProjectionBuilder = dslContext.builderFactory().score();
	}

	@Override
	public SearchProjection<Float> toProjection() {
		return scoreProjectionBuilder.build();
	}

}
