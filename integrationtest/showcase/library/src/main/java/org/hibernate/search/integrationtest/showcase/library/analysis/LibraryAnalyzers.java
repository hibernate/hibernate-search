/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.analysis;

public final class LibraryAnalyzers {

	public static final String NORMALIZER_SORT = "asciifolding_lowercase";
	public static final String NORMALIZER_ISBN = "isbn";

	private LibraryAnalyzers() {
	}
}
