/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.formatter.impl;

public final class IntegerFieldFormatter implements LuceneFieldFormatter<Integer> {

	public static final IntegerFieldFormatter INSTANCE = new IntegerFieldFormatter();

	private IntegerFieldFormatter() {
	}

	@Override
	public Integer format(Object value) {
		return (Integer) value;
	}
}
