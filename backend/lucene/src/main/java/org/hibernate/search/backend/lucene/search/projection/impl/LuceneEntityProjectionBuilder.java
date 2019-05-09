/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;


public class LuceneEntityProjectionBuilder<E> implements EntityProjectionBuilder<E> {

	@SuppressWarnings("rawtypes")
	private static final LuceneEntityProjectionBuilder INSTANCE = new LuceneEntityProjectionBuilder();

	@SuppressWarnings("unchecked")
	public static <T> LuceneEntityProjectionBuilder<T> get() {
		return INSTANCE;
	}

	private LuceneEntityProjectionBuilder() {
	}

	@Override
	public SearchProjection<E> build() {
		return LuceneEntityProjection.get();
	}
}
