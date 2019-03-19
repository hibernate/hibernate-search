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
 *
 * <h3>Common concepts</h3>
 *
 * <h4 id="commonconcepts-parametertype">Type of predicate parameters</h4>
 *
 * Some predicates, such as the {@link #match()} predicate or the {@link #range()} predicate,
 * target an index field and define an {@link Object} parameter ({@link MatchPredicateFieldSetContext#matching(Object)},
 * {@link RangePredicateFieldSetContext#from(Object)}, ...).
 * The type of arguments passed to these methods is expected to be consistent with the targeted field:
 * if you want a field "myField" to be between values {@code X} and {@code Y},
 * Hibernate Search needs to be able to convert the values {@code X} and {@code Y}
 * to something comparable to the indexed value of "myField".
 * <p>
 * Generally the expected type of this argument should be rather obvious:
 * for example if you created a field by mapping an {@link Integer} property,
 * then an {@link Integer} value will be expected when building a predicate;
 * if you mapped a {@link java.time.LocalDate}, then a {@link java.time.LocalDate} will be expected.
 * Note that the type that is actually stored in the index does not matter:
 * if you mapped a custom enum type, then this same enum type will be expected throughout the predicate DSL,
 * even if it is stored as a {@link String} in the index.
 * <p>
 * Note it is possible to skip some of the conversion performed by Hibernate Search and by targeting "raw" fields,
 * and in that case the expected type of arguments will be different;
 * see <a href="#commonconcepts-rawfield">Raw fields</a> for details.
 * <p>
 * Sometimes a predicate targets <em>multiple</em> fields, either explicitly
 * (multiple field names passed to the {@code onFields} method when defining the predicate)
 * or implicitly (multiple targeted indexes).
 * In that case, the type of the targeted fields must be compatible.
 * For example targeting an {@link Integer} field and a {@link java.time.LocalDate} field
 * in the same {@link #match()} predicate won't work, because you won't be able to pass a non-null argument
 * that is both an {@link Integer} and a {@link java.time.LocalDate}
 * to the {@link MatchPredicateFieldSetContext#matching(Object)} method.
 * Thus you generally cannot target fields with different types in the same predicate.
 * When targeting different fields in the same index, you can, however,
 * define one predicate for each field and combine them using a {@link #bool() boolean predicate}.
 * <p>
 * Note that custom bridges have the ability to customize the expected type of arguments in the DSL:
 * for example they could accept {@link String} arguments when targeting {@link Integer} fields.
 * See the documentation of bridges for more information on custom bridges.
 *
 * <h4 id="commonconcepts-rawfield">Raw fields</h4>
 *
 * Some predicates allow to target "raw fields" using an {@code onRawField(String)} method instead of {@code onField(String)}.
 * Targeting a raw field means some of the usual conversion will be skipped when Hibernate Search processes
 * predicate arguments such as the value to match.
 * <p>
 * This is useful when the type of the indexed property is not the same as the type of search arguments.
 * For example one may use a custom bridge implementing {@code ValueBridge<MyType, String>}
 * to translate a property from a custom type to a string when indexing.
 * When targeting that field with {@code onField},
 * Hibernate Search will expect predicate arguments to be instances of the custom type;
 * with {@code onRawField}, it will expect strings.
 * <p>
 * For details about conversions applied by built-in bridges,
 * please refer to the reference documentation.
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
