/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;


public class FieldSearchProjectionBuilderImpl<T> implements FieldSearchProjectionBuilder<T> {

	private final String absoluteFieldPath;

	private final Class<T> type;

	public FieldSearchProjectionBuilderImpl(String absoluteFieldPath, Class<T> type) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.type = type;
	}

	@Override
	public SearchProjection<T> build() {
		return new FieldSearchProjectionImpl<>( absoluteFieldPath, type );
	}
}
