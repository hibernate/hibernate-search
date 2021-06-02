/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import java.util.Locale;

import org.hibernate.search.util.common.SearchException;

public interface StubSearchQueryElementFactory<T> {

	T create(StubSearchIndexScope scope, StubSearchIndexSchemaElementContext element);

	default void checkCompatibleWith(StubSearchQueryElementFactory<?> other) {
		if ( !equals( other ) ) {
			throw new SearchException(
					String.format( Locale.ROOT, "Incompatible factories: '%s' vs. '%s'", this, other ) );
		}
	}

}
