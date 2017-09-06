/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl;


/**
 * @author Yoann Rodiere
 */
public interface MatchClauseContext<N> {

	// TODO wildcard, fuzzy. Or maybe a separate context?

	default MatchClauseFieldSetContext<N> onField(String fieldName) {
		return onFields( fieldName );
	}

	MatchClauseFieldSetContext<N> onFields(String ... fieldNames);

}
