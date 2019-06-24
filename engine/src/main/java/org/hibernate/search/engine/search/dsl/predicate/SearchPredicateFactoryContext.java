/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


import java.util.function.Consumer;

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
	 * @return The initial step of a DSL where the "match all" predicate can be defined.
	 * @see MatchAllPredicateOptionsStep
	 */
	MatchAllPredicateOptionsStep matchAll();

	/**
	 * Match documents where the identifier is among the given values.
	 *
	 * @return The initial step of a DSL allowing the definition of an "id" predicate.
	 * @see MatchIdPredicateMatchingStep
	 */
	MatchIdPredicateMatchingStep id();

	/**
	 * Match documents if they match a combination of boolean clauses.
	 *
	 * @return The initial step of a DSL where the "boolean" predicate can be defined.
	 * @see BooleanPredicateClausesStep
	 */
	BooleanPredicateClausesStep bool();

	/**
	 * Match documents if they match a combination of boolean clauses,
	 * which will be defined by the given consumer.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A consumer that will add clauses to the step passed in parameter.
	 * Should generally be a lambda expression.
	 * @return The final step of the boolean predicate definition.
	 * @see BooleanPredicateClausesStep
	 */
	PredicateFinalStep bool(Consumer<? super BooleanPredicateClausesStep> clauseContributor);

	/**
	 * Match documents where targeted fields have a value that "matches" a given single value.
	 * <p>
	 * Note that "value matching" may be exact or approximate depending on the type of the targeted fields:
	 * numeric fields in particular imply exact matches,
	 * while analyzed, full-text fields imply approximate matches depending on how they are analyzed.
	 *
	 * @return The initial step of a DSL where the "match" predicate can be defined.
	 * @see MatchPredicateFieldStep
	 */
	MatchPredicateFieldStep match();

	/**
	 * Match documents where targeted fields have a value within lower and upper bounds.
	 *
	 * @return The initial step of a DSL where the "range" predicate can be defined.
	 * @see RangePredicateFieldStep
	 */
	RangePredicateFieldStep range();

	/**
	 * Match documents where targeted fields have a value that contains a given phrase.
	 *
	 * @return The initial step of a DSL where the "phrase" predicate can be defined.
	 * @see PhrasePredicateFieldStep
	 */
	PhrasePredicateFieldStep phrase();

	/**
	 * Match documents where targeted fields contain a term that matches a given pattern,
	 * such as {@code inter*on} or {@code pa?t}.
	 * <p>
	 * Note that such patterns are <strong>not analyzed</strong>,
	 * thus any character that is not a wildcard must match exactly the content of the index
	 * (including uppercase letters, diacritics, ...).
	 *
	 * @return The initial step of a DSL where the "wildcard" predicate can be defined.
	 * @see WildcardPredicateFieldStep
	 */
	WildcardPredicateFieldStep wildcard();

	/**
	 * Match documents where a
	 * {@link org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage#NESTED nested object}
	 * matches a given predicate.
	 *
	 * @return The initial step of a DSL where the "nested" predicate can be defined.
	 * @see NestedPredicateFieldStep
	 */
	NestedPredicateFieldStep nested();

	/**
	 * Match documents according to a given query string,
	 * with a simple query language adapted to end users.
	 * <p>
	 * Note that by default, unless the query string contains explicit operators,
	 * documents will match if <em>any</em> term mentioned in the query string is present in the document (OR operator).
	 * This makes sense when sorting results by relevance, but is not ideal otherwise.
	 * See {@link SimpleQueryStringPredicateOptionsStep#withAndAsDefaultOperator()} to change this behavior.
	 *
	 * @return The initial step of a DSL where the "simple query string" predicate can be defined.
	 * @see SimpleQueryStringPredicateFieldStep
	 */
	SimpleQueryStringPredicateFieldStep simpleQueryString();

	/**
	 * Match documents where a given field exists.
	 * <p>
	 * Fields are considered to exist in a document when they have at least one non-null value in this document.
	 *
	 * @return The initial step of a DSL where the "exists" predicate can be defined.
	 * @see ExistsPredicateFieldStep
	 */
	ExistsPredicateFieldStep exists();

	/**
	 * Access the different types of spatial predicates.
	 *
	 * @return The initial step of a DSL where spatial predicates can be defined.
	 * @see SpatialPredicateInitialStep
	 */
	SpatialPredicateInitialStep spatial();

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
	 * Create a DSL step allowing multiple attempts to apply extensions one after the other,
	 * failing only if <em>none</em> of the extensions is supported.
	 * <p>
	 * If you only need to apply a single extension and fail if it is not supported,
	 * use the simpler {@link #extension(SearchPredicateFactoryContextExtension)} method instead.
	 *
	 * @return A DSL step.
	 */
	SearchPredicateFactoryContextExtensionStep extension();

}
