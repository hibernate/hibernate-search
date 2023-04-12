/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types;

/**
 * Whether we want to be able to highlight this field.
 */
public enum Highlightable {
	/**
	 * Use the backend-specific default that is dependent on an overall field configuration.
	 * <ul>
	 *     <li>
	 *         Lucene's default value is dependent on the {@link Projectable projectable value} configured for the field.
	 *         If the field is {@link Projectable#YES projectable} then {@code [PLAIN, UNIFIED]} highlighters are supported.
	 *         {@link Projectable#NO Otherwise} it defaults to {@code NO}.
	 *     </li>
	 *     <li>Elasticsearch's default value is {@code [PLAIN, UNIFIED]} </li>
	 *     <li>
	 *         Additionally, if the {@link TermVector term vector storing strategy} is set to {@link TermVector#WITH_POSITIONS_OFFSETS}
	 *         or {@link TermVector#WITH_POSITIONS_OFFSETS_PAYLOADS} both backends, would support the {@code FAST_VECTOR}
	 *         highlighter, if they already support the other two ({@code [PLAIN, UNIFIED]}).
	 *     </li>
	 * </ul>
	 */
	DEFAULT,
	/**
	 * Do not allow highlighting on the field.
	 */
	NO,
	/**
	 * Allow any highlighter type be applied for highlighting the field.
	 */
	ANY,
	/**
	 * Allow the plain highlighter type be applied for highlighting the field.
	 */
	PLAIN,
	/**
	 * Allow the unified highlighter type be applied for highlighting the field.
	 */
	UNIFIED,
	/**
	 * Allow the fast vector highlighter type be applied for highlighting the field.
	 */
	FAST_VECTOR
}
