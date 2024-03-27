/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.highlighter.dsl;

/**
 * Factory for search highlighters.
 * <p>
 * This is the main entry point to the highlighter DSL.
 * <p>
 * Please refer to the documentation of your particular backend for any details on specific highlighter types
 * provided through this factory.
 */
public interface SearchHighlighterFactory {

	/**
	 * @return the initial step to configure a highlighter of a plain type.
	 */
	HighlighterPlainOptionsStep plain();

	/**
	 * @return the initial step to configure a highlighter of a unified type.
	 */
	HighlighterUnifiedOptionsStep unified();

	/**
	 * @return the initial step to configure a highlighter of a fast vector type.
	 */
	HighlighterFastVectorHighlighterOptionsStep fastVector();

}
