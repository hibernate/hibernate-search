/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.highlighter.dsl;

/**
 * Defines a set of predefined pre-/post- tags.
 */
public enum HighlighterTagSchema {
	/**
	 * Defines pre tags as: {@code <em class="hlt1">, <em class="hlt2">, <em class="hlt3">, <em class="hlt4">, <em class="hlt5">, <em class="hlt6">, <em class="hlt7">, <em class="hlt8">, <em class="hlt9">, <em class="hlt10">}
	 * and post tag as {@code </em>}
	 */
	STYLED;
}
