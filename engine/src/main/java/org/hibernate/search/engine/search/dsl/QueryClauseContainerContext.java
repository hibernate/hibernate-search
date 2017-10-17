/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl;


import java.util.function.Consumer;

/**
 * @author Yoann Rodiere
 */
public interface QueryClauseContainerContext<N> {

	BooleanJunctionContext<N> bool();

	MatchClauseContext<N> match();

	RangeClauseContext<N> range();

	// TODO ids query (Type + list of IDs? Just IDs? See https://www.elastic.co/guide/en/elasticsearch/reference/5.5/query-dsl-ids-query.html)
	// TODO other queries (spatial, ...)

	<T> T withExtension(QueryClauseExtension<N, T> extension);

	<T> N withExtensionOptional(QueryClauseExtension<N, T> extension, Consumer<T> clauseContributor);

	<T> N withExtensionOptional(QueryClauseExtension<N, T> extension, Consumer<T> clauseContributor,
			Consumer<QueryClauseContainerContext<N>> fallbackClauseContributor);

}
