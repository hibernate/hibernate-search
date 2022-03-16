/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;

abstract class AbstractLuceneProjection<P> implements LuceneSearchProjection<P> {

	private final Set<String> indexNames;

	AbstractLuceneProjection(AbstractBuilder<?> builder) {
		this( builder.scope );
	}

	AbstractLuceneProjection(LuceneSearchIndexScope<?> scope) {
		this.indexNames = scope.hibernateSearchIndexNames();
	}

	@Override
	public final Set<String> indexNames() {
		return indexNames;
	}

	abstract static class AbstractBuilder<P> implements SearchProjectionBuilder<P> {
		protected final LuceneSearchIndexScope<?> scope;

		AbstractBuilder(LuceneSearchIndexScope<?> scope) {
			this.scope = scope;
		}
	}

}
