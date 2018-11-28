/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.ReferenceProjectionBuilder;


public class LuceneReferenceProjectionBuilder<R> implements ReferenceProjectionBuilder<R> {

	@SuppressWarnings("rawtypes")
	private static final LuceneReferenceProjectionBuilder INSTANCE = new LuceneReferenceProjectionBuilder();

	@SuppressWarnings("unchecked")
	public static <T> LuceneReferenceProjectionBuilder<T> get() {
		return INSTANCE;
	}

	private LuceneReferenceProjectionBuilder() {
	}

	@Override
	public SearchProjection<R> build() {
		return LuceneReferenceProjection.get();
	}
}
