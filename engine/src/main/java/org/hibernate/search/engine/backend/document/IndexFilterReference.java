/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document;

import org.hibernate.search.engine.search.predicate.factories.FilterFactory;

/**
 * A reference to an "object" filter of an indexed document.
 *
 * @param <F> The indexed filter factory type.
 *
 */
public interface IndexFilterReference<F extends FilterFactory> {

	String getName();

	F getFactory();
}
