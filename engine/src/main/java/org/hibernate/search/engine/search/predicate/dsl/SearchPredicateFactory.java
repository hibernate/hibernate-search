/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;


import java.util.function.Consumer;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A factory for search predicates.
 * <p>
 * This is the main entry point to the predicate DSL.
 *
 * <h2 id="field-paths">Field paths</h2>
 *
 * By default, field paths passed to this DSL are interpreted as absolute,
 * i.e. relative to the index root.
 * <p>
 * However, a new, "relative" factory can be created with {@link #withRoot(String)}:
 * the new factory interprets paths as relative to the object field passed as argument to the method.
 * <p>
 * This can be useful when calling reusable methods that can apply the same predicate
 * on different object fields that have same structure (same sub-fields).
 * <p>
 * Such a factory can also transform relative paths into absolute paths using {@link #toAbsolutePath(String)};
 * this can be useful for native predicates in particular.
 */
public interface SearchPredicateFactory {

	/**
	 * Match all documents.
	 *
	 * @return The initial step of a DSL where the "match all" predicate can be defined.
	 * @see MatchAllPredicateOptionsStep
	 */
	MatchAllPredicateOptionsStep<?> matchAll();

	/**
	 * Match none of the documents.
	 *
	 * @return The initial step of a DSL where the "match none" predicate can be defined.
	 * @see MatchNonePredicateFinalStep
	 */
	MatchNonePredicateFinalStep matchNone();

	/**
	 * Match documents where the identifier is among the given values.
	 *
	 * @return The initial step of a DSL allowing the definition of an "id" predicate.
	 * @see MatchIdPredicateMatchingStep
	 */
	MatchIdPredicateMatchingStep<?> id();

	/**
	 * Match documents if they match a combination of boolean clauses.
	 *
	 * @return The initial step of a DSL where the "boolean" predicate can be defined.
	 * @see BooleanPredicateClausesStep
	 */
	BooleanPredicateClausesStep<?> bool();

	/**
	 * Match documents if they match a combination of boolean clauses,
	 * which will be defined by the given consumer.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A consumer that will add clauses to the step passed in parameter.
	 * Should generally be a lambda expression.
	 * @return The final step of the boolean predicate definition.
	 * @deprecated Use {@code .bool().with(...)} instead.
	 * @see BooleanPredicateClausesStep#with(Consumer)
	 */
	@Deprecated
	PredicateFinalStep bool(Consumer<? super BooleanPredicateClausesStep<?>> clauseContributor);

	/**
	 * Match documents if they match all inner clauses.
	 *
	 * @return The initial step of a DSL where predicates that must match can be added and options can be set.
	 * @see GenericSimpleBooleanPredicateClausesStep
	 */
	SimpleBooleanPredicateClausesStep<?> and();

	/**
	 * Match documents if they match all previously-built {@link SearchPredicate}.
	 *
	 * @return The step of a DSL where options can be set.
	 */
	SimpleBooleanPredicateOptionsStep<?> and(
			SearchPredicate firstSearchPredicate,
			SearchPredicate... otherSearchPredicates);

	/**
	 * Match documents if they match all clauses.
	 *
	 * @return The step of a DSL where options can be set.
	 */
	SimpleBooleanPredicateOptionsStep<?> and(PredicateFinalStep firstSearchPredicate,
			PredicateFinalStep... otherSearchPredicates);

	/**
	 * Match documents if they match any inner clause.
	 *
	 * @return The initial step of a DSL where predicates that should match can be added and options can be set.
	 * @see GenericSimpleBooleanPredicateClausesStep
	 */
	SimpleBooleanPredicateClausesStep<?> or();

	/**
	 * Match documents if they match any previously-built {@link SearchPredicate}.
	 *
	 * @return The step of a DSL where options can be set.
	 */
	SimpleBooleanPredicateOptionsStep<?> or(SearchPredicate firstSearchPredicate,
			SearchPredicate... otherSearchPredicates);

	/**
	 * Match documents if they match any clause.
	 *
	 * @return The step of a DSL where options can be set.
	 */
	SimpleBooleanPredicateOptionsStep<?> or(PredicateFinalStep firstSearchPredicate,
			PredicateFinalStep... otherSearchPredicates);

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
	MatchPredicateFieldStep<?> match();

	/**
	 * Match documents where targeted fields have a value within lower and upper bounds.
	 *
	 * @return The initial step of a DSL where the "range" predicate can be defined.
	 * @see RangePredicateFieldStep
	 */
	RangePredicateFieldStep<?> range();

	/**
	 * Match documents where targeted fields have a value that contains a given phrase.
	 *
	 * @return The initial step of a DSL where the "phrase" predicate can be defined.
	 * @see PhrasePredicateFieldStep
	 */
	PhrasePredicateFieldStep<?> phrase();

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
	WildcardPredicateFieldStep<?> wildcard();

	/**
	 * Match documents where targeted fields contain a term that matches a given regular expression.
	 *
	 * @return The initial step of a DSL where the "regexp" predicate can be defined.
	 * @see RegexpPredicateFieldStep
	 */
	RegexpPredicateFieldStep<?> regexp();

	/**
	 * Match documents where targeted fields contain a term that matches some terms of a given series of terms.
	 *
	 * @return The initial step of a DSL where the "terms" predicate can be defined.
	 * @see TermsPredicateFieldStep
	 */
	TermsPredicateFieldStep<?> terms();

	/**
	 * Match documents where a {@link ObjectStructure#NESTED nested object} matches a given predicate.
	 *
	 * @return The initial step of a DSL where the "nested" predicate can be defined.
	 * @see NestedPredicateFieldStep
	 * @deprecated Use {@link #nested(String)} instead.
	 */
	@Deprecated
	NestedPredicateFieldStep<?> nested();

	/**
	 * Match documents where a {@link ObjectStructure#NESTED nested object} matches inner predicates
	 * to be defined in the next steps.
	 * <p>
	 * The resulting nested predicate must match <em>all</em> inner clauses,
	 * similarly to an {@link #and() "and" predicate}.
	 *
	 * @param objectFieldPath The <a href="#field-paths">path</a> to the (nested) object field that must match.
	 * @return The initial step of a DSL where the "nested" predicate can be defined.
	 * @see NestedPredicateFieldStep
	 */
	NestedPredicateClausesStep<?> nested(String objectFieldPath);

	/**
	 * Match documents according to a given query string,
	 * with a simple query language adapted to end users.
	 * <p>
	 * Note that by default, unless the query string contains explicit operators,
	 * documents will match if <em>any</em> term mentioned in the query string is present in the document (OR operator).
	 * This makes sense when sorting results by relevance, but is not ideal otherwise.
	 * See {@link SimpleQueryStringPredicateOptionsStep#defaultOperator(BooleanOperator)} to change this behavior.
	 *
	 * @return The initial step of a DSL where the "simple query string" predicate can be defined.
	 * @see SimpleQueryStringPredicateFieldStep
	 */
	SimpleQueryStringPredicateFieldStep<?> simpleQueryString();

	/**
	 * Match documents where a given field exists.
	 * <p>
	 * Fields are considered to exist in a document when they have at least one non-null value in this document.
	 *
	 * @return The initial step of a DSL where the "exists" predicate can be defined.
	 * @see ExistsPredicateFieldStep
	 */
	ExistsPredicateFieldStep<?> exists();

	/**
	 * Access the different types of spatial predicates.
	 *
	 * @return The initial step of a DSL where spatial predicates can be defined.
	 * @see SpatialPredicateInitialStep
	 */
	SpatialPredicateInitialStep spatial();

	/**
	 * Match documents if they match a combination of defined named predicate clauses.
	 *
	 * @param path The <a href="#field-paths">path</a> to the named predicate,
	 * formatted as {@code <object field path>.<predicate name>},
	 * or just {@code <predicate name>} if the predicate was declared at the root.
	 * @return The initial step of a DSL where named predicate predicates can be defined.
	 * @see NamedPredicateOptionsStep
	 */
	@Incubating
	NamedPredicateOptionsStep named(String path);

	/**
	 * Extend the current factory with the given extension,
	 * resulting in an extended factory offering different types of predicates.
	 *
	 * @param extension The extension to the predicate DSL.
	 * @param <T> The type of factory provided by the extension.
	 * @return The extended factory.
	 * @throws SearchException If the extension cannot be applied (wrong underlying backend, ...).
	 */
	<T> T extension(SearchPredicateFactoryExtension<T> extension);

	/**
	 * Create a DSL step allowing multiple attempts to apply extensions one after the other,
	 * failing only if <em>none</em> of the extensions is supported.
	 * <p>
	 * If you only need to apply a single extension and fail if it is not supported,
	 * use the simpler {@link #extension(SearchPredicateFactoryExtension)} method instead.
	 *
	 * @return A DSL step.
	 */
	SearchPredicateFactoryExtensionIfSupportedStep extension();

	/**
	 * Create a new predicate factory whose root for all paths passed to the DSL
	 * will be the given object field.
	 * <p>
	 * This is used to call reusable methods that apply the same predicate
	 * on different object fields that have same structure (same sub-fields).
	 *
	 * @param objectFieldPath The path from the current root to an object field that will become the new root.
	 * @return A new predicate factory using the given object field as root.
	 */
	@Incubating
	SearchPredicateFactory withRoot(String objectFieldPath);

	/**
	 * @param relativeFieldPath The path to a field, relative to the {@link #withRoot(String) root} of this factory.
	 * @return The absolute path of the field, for use in native predicates for example.
	 * Note the path is returned even if the field doesn't exist.
	 */
	@Incubating
	String toAbsolutePath(String relativeFieldPath);

}
