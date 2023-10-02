/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.analysis;

public final class Analyzers {

	private Analyzers() {
	}

	/**
	 * tokenizer: standard
	 * token filters: lowercase, snowball-porter(english), asciifolding
	 */
	public static final String ANALYZER_ENGLISH = "english";

	/**
	 * token filters: lowercase, asciifolding
	 */
	public static final String NORMALIZER_ENGLISH = "english";

}
