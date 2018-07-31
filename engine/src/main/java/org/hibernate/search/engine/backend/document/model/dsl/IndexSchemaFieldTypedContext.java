/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;

/**
 * @param <F> The type of field values.
 */
public interface IndexSchemaFieldTypedContext<F> extends IndexSchemaFieldTerminalContext<F> {

	// TODO add common options: stored, sortable, ...

	IndexSchemaFieldTypedContext<F> analyzer(String analyzerName);

	IndexSchemaFieldTypedContext<F> normalizer(String normalizerName);

	IndexSchemaFieldTypedContext<F> store(Store store);

	IndexSchemaFieldTypedContext<F> sortable(Sortable sortable);

}
