/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.ObjectProjectionBuilder;


public class LuceneObjectProjectionBuilder<O> implements ObjectProjectionBuilder<O> {

	@SuppressWarnings("rawtypes")
	private static final LuceneObjectProjectionBuilder INSTANCE = new LuceneObjectProjectionBuilder();

	@SuppressWarnings("unchecked")
	public static <T> LuceneObjectProjectionBuilder<T> get() {
		return INSTANCE;
	}

	private LuceneObjectProjectionBuilder() {
	}

	@Override
	public SearchProjection<O> build() {
		return LuceneObjectProjection.get();
	}
}
