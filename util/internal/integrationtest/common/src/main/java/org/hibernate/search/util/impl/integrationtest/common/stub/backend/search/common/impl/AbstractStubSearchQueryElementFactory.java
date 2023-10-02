/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import java.util.Locale;

import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;
import org.hibernate.search.util.common.SearchException;

public abstract class AbstractStubSearchQueryElementFactory<T>
		implements SearchQueryElementFactory<T, StubSearchIndexScope, StubSearchIndexNodeContext> {

	@Override
	public void checkCompatibleWith(SearchQueryElementFactory<?, ?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			throw new SearchException(
					String.format( Locale.ROOT, "Incompatible factories: '%s' vs. '%s'", this, other ) );
		}
	}

}
