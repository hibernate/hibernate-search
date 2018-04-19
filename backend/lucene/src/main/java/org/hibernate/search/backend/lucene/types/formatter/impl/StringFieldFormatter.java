/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.formatter.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldFormatter;

public final class StringFieldFormatter implements LuceneFieldFormatter<String> {

	public static final StringFieldFormatter INSTANCE = new StringFieldFormatter();

	private StringFieldFormatter() {
	}

	@Override
	public String format(Object value) {
		return (String) value;
	}
}
