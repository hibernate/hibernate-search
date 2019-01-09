/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.dsl;

import org.hibernate.search.engine.backend.types.IndexFieldType;

/**
 * @param <F> The Java type of values held by fields.
 */
public interface IndexFieldTypeTerminalContext<F> {

	IndexFieldType<F> toIndexFieldType();

}
