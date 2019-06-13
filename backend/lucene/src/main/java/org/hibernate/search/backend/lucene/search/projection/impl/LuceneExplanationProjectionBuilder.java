/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;

import org.apache.lucene.search.Explanation;


public class LuceneExplanationProjectionBuilder implements SearchProjectionBuilder<Explanation> {

	private final Set<String> indexNames;

	public LuceneExplanationProjectionBuilder(Set<String> indexNames) {
		this.indexNames = indexNames;
	}

	@Override
	public SearchProjection<Explanation> build() {
		return new LuceneExplanationProjection( indexNames );
	}
}
