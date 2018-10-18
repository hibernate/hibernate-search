/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.ScoreProjectionContext;
import org.hibernate.search.engine.search.projection.spi.ScoreSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionFactory;


public class ScoreProjectionContextImpl implements ScoreProjectionContext {

	private ScoreSearchProjectionBuilder scoreProjectionBuilder;

	ScoreProjectionContextImpl(SearchProjectionFactory factory) {
		this.scoreProjectionBuilder = factory.score();
	}

	@Override
	public SearchProjection<Float> toProjection() {
		return scoreProjectionBuilder.build();
	}

}
