/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;

public abstract class AbstractLuceneReversibleSort extends AbstractLuceneSort {

	protected final SortOrder order;

	protected AbstractLuceneReversibleSort(AbstractBuilder builder) {
		super( builder );
		order = builder.order;
	}

	public abstract static class AbstractBuilder extends AbstractLuceneSort.AbstractBuilder {
		protected SortOrder order;

		protected AbstractBuilder(LuceneSearchIndexScope<?> scope) {
			super( scope );
		}

		public void order(SortOrder order) {
			this.order = order;
		}
	}
}
