/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;

public abstract class AbstractLuceneReversibleSort extends AbstractLuceneSort {

	protected final SortOrder order;

	protected AbstractLuceneReversibleSort(AbstractBuilder builder) {
		super( builder );
		order = builder.order;
	}

	public abstract static class AbstractBuilder extends AbstractLuceneSort.AbstractBuilder {
		protected SortOrder order;

		protected AbstractBuilder(LuceneSearchContext searchContext) {
			super( searchContext );
		}

		public void order(SortOrder order) {
			this.order = order;
		}
	}
}
