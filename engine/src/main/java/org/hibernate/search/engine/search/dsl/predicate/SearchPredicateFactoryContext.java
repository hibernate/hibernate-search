/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.util.common.SearchException;

/**
 * A context allowing to specify the type of a predicate.
 * <p>
 * This is the main entry point to the predicate DSL.
 */
public interface SearchPredicateFactoryContext {

	/**
	 * Match all documents.
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#toPredicate() get the resulting predicate}.
	 * @see MatchAllPredicateContext
	 */
	MatchAllPredicateContext matchAll();

	/**
	 * Match documents where the identifier is among the given values.
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#toPredicate() get the resulting predicate}.
	 * @see MatchIdPredicateContext
	 */
	MatchIdPredicateContext id();

	/**
	 * Match documents if they match a combination of boolean clauses.
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#toPredicate() get the resulting predicate}.
	 * @see BooleanJunctionPredicateContext
	 */
	BooleanJunctionPredicateContext bool();

	/**
	 * Match documents if they match a combination of boolean clauses,
	 * which will be defined by the given consumer.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A consumer that will add clauses to the context passed in parameter.
	 * Should generally be a lambda expression.
	 * @return The resulting {@link SearchPredicate}
	 * @see BooleanJunctionPredicateContext
	 */
	SearchPredicateTerminalContext bool(Consumer<? super BooleanJunctionPredicateContext> clauseContributor);

	/**
	 * Match documents where targeted fields have a value that "matches" a given single value.
	 * <p>
	 * Note that "value matching" may be exact or approximate depending on the type of the targeted fields:
	 * numeric fields in particular imply exact matches,
	 * while analyzed, full-text fields imply approximate matches depending on how they are analyzed.
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#toPredicate() get the resulting predicate}.
	 * @see MatchPredicateContext
	 */
	MatchPredicateContext match();

	/**
	 * Match documents where targeted fields have a value within lower and upper bounds.
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#toPredicate() get the resulting predicate}.
	 * @see RangePredicateContext
	 */
	RangePredicateContext range();

	/**
	 * Match documents where targeted fields have a value that contains a given phrase.
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#toPredicate() get the resulting predicate}.
	 * @see PhrasePredicateContext
	 */
	PhrasePredicateContext phrase();

	/**
	 * Match documents where targeted fields contain a term that matches a given pattern,
	 * such as {@code inter*on} or {@code pa?t}.
	 * <p>
	 * Note that such patterns are <strong>not analyzed</strong>,
	 * thus any character that is not a wildcard must match exactly the content of the index
	 * (including uppercase letters, diacritics, ...).
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#toPredicate() get the resulting predicate}.
	 * @see WildcardPredicateContext
	 */
	WildcardPredicateContext wildcard();

	/**
	 * Match documents where a
	 * {@link org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage#NESTED nested object}
	 * matches a given predicate.
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#toPredicate() get the resulting predicate}.
	 * @see NestedPredicateContext
	 */
	NestedPredicateContext nested();

	/**
	 * Match documents according to a given query string,
	 * with a simple query language adapted to end users.
	 * <p>
	 * Note that by default, unless the query string contains explicit operators,
	 * documents will match if <em>any</em> term mentioned in the query string is present in the document (OR operator).
	 * This makes sense when sorting results by relevance, but is not ideal otherwise.
	 * See {@link SimpleQueryStringPredicateTerminalContext#withAndAsDefaultOperator()} to change this behavior.
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#toPredicate() get the resulting predicate}.
	 * @see SimpleQueryStringPredicateContext
	 */
	SimpleQueryStringPredicateContext simpleQueryString();

	/**
	 * Match documents where a given field exists.
	 * <p>
	 * Fields are considered to exist in a document when they have at least one non-null value in this document.
	 *
	 * @return A context allowing to define the predicate more precisely
	 * and ultimately {@link SearchPredicateTerminalContext#toPredicate() get the resulting predicate}.
	 * @see ExistsPredicateContext
	 */
	ExistsPredicateContext exists();

	/**
	 * Access the different types of spatial predicates.
	 *
	 * @return A context allowing to define the type of spatial predicate.
	 * @see SpatialPredicateContext
	 */
	SpatialPredicateContext spatial();

	/**
	 * Extend the current context with the given extension,
	 * resulting in an extended context offering different types of predicates.
	 *
	 * @param extension The extension to the predicate DSL.
	 * @param <T> The type of context provided by the extension.
	 * @return The extended context.
	 * @throws SearchException If the extension cannot be applied (wrong underlying backend, ...).
	 */
	<T> T extension(SearchPredicateFactoryContextExtension<T> extension);

	/**
	 * Create a context allowing to try to apply multiple extensions one after the other,
	 * failing only if <em>none</em> of the extensions is supported.
	 * <p>
	 * If you only need to apply a single extension and fail if it is not supported,
	 * use the simpler {@link #extension(SearchPredicateFactoryContextExtension)} method instead.
	 *
	 * @return A context allowing to define the extensions to attempt, and the corresponding predicates.
	 */
	SearchPredicateFactoryExtensionContext extension();

}
