/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
