/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.ReferenceSearchProjectionBuilder;


public class ReferenceSearchProjectionBuilderImpl<R> implements ReferenceSearchProjectionBuilder<R> {

	@SuppressWarnings("rawtypes")
	private static final ReferenceSearchProjectionBuilderImpl INSTANCE = new ReferenceSearchProjectionBuilderImpl();

	@SuppressWarnings("unchecked")
	public static <T> ReferenceSearchProjectionBuilderImpl<T> get() {
		return INSTANCE;
	}

	private ReferenceSearchProjectionBuilderImpl() {
	}

	@Override
	public SearchProjection<R> build() {
		return ReferenceSearchProjectionImpl.get();
	}
}
