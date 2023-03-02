/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.highlighter.dsl;

/**
 * Defines how to encode the output snippets.
 */
public enum HighlighterEncoder {
	/**
	 * Text will be returned as is with no encoding.
	 */
	DEFAULT,
	/**
	 * Simple HTML escaping will be applied before the highlight tags are inserted into the output.
	 */
	HTML;
}
