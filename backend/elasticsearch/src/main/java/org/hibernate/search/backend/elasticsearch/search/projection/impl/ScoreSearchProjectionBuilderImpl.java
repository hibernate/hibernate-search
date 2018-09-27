/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.ScoreSearchProjectionBuilder;


public class ScoreSearchProjectionBuilderImpl implements ScoreSearchProjectionBuilder {

	private static final ScoreSearchProjectionBuilderImpl INSTANCE = new ScoreSearchProjectionBuilderImpl();

	public static ScoreSearchProjectionBuilderImpl get() {
		return INSTANCE;
	}

	private ScoreSearchProjectionBuilderImpl() {
	}

	@Override
	public SearchProjection<Float> build() {
		return ScoreSearchProjectionImpl.get();
	}
}
