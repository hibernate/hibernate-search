/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.highlighter.dsl;

/**
 * Defines how to break up text into highlight snippets.
 */
public enum HighlighterFragmenter {
	/**
	 * Breaks up text into same-sized fragments.
	 */
	SIMPLE,
	/**
	 * Breaks up text into same-sized fragments, but tries to avoid breaking up a phrase to be highlighted.
	 */
	SPAN;
}
