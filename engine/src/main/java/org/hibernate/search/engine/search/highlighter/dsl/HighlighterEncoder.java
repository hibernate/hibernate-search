/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
