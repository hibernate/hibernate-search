/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.formatter.impl;

public final class SimpleCastingFieldFormatter<T> implements LuceneFieldFormatter<T> {

	public SimpleCastingFieldFormatter() {
	}

	@SuppressWarnings("unchecked")
	@Override
	public T format(Object value) {
		return (T) value;
	}
}
