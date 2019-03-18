/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;

/**
 * @param <R> The reference type.
 */
public interface IndexSchemaFieldTerminalContext<R> {

	// FIXME Remove this method
	@Deprecated
	default R createAccessor() {
		return toReference();
	}

	R toReference();

}
