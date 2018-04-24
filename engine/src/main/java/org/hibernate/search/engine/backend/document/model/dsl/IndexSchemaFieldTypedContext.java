/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;

/**
 * @author Yoann Rodiere
 */
public interface IndexSchemaFieldTypedContext<T> extends IndexSchemaFieldTerminalContext<T> {

	// TODO add common options: stored, sortable, ...

	IndexSchemaFieldTypedContext<T> analyzer(String analyzerName);

	IndexSchemaFieldTypedContext<T> normalizer(String normalizerName);

	IndexSchemaFieldTypedContext<T> store(Store store);

	IndexSchemaFieldTypedContext<T> sortable(Sortable sortable);

}
