/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;

abstract class AbstractLuceneProjection<E, P> implements LuceneSearchProjection<E, P> {

	private final Set<String> indexNames;

	AbstractLuceneProjection(AbstractBuilder<?> builder) {
		this( builder.searchContext );
	}

	AbstractLuceneProjection(LuceneSearchContext searchContext) {
		this.indexNames = searchContext.indexes().indexNames();
	}

	@Override
	public final Set<String> indexNames() {
		return indexNames;
	}

	abstract static class AbstractBuilder<P> implements SearchProjectionBuilder<P> {
		protected final LuceneSearchContext searchContext;

		AbstractBuilder(LuceneSearchContext searchContext) {
			this.searchContext = searchContext;
		}
	}

}
