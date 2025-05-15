/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;


import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.reference.predicate.NestedPredicateFieldReference;
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
 *
 * <h2 id="field-references">Field references</h2>
 *
 * A {@link org.hibernate.search.engine.search.reference field reference} is always represented by the absolute field path and,
 * if applicable, i.e. when a field reference is typed, a combination of the {@link org.hibernate.search.engine.search.common.ValueModel} and the type.
 * <p>
 * Field references are usually accessed from the generated Hibernate Search's static metamodel classes that describe the index structure.
 * Such reference provides the information on which search capabilities the particular index field possesses, and allows switching between different
 * {@link org.hibernate.search.engine.search.common.ValueModel value model representations}.
 *
 * @param <SR> Scope root type.
 */
public interface TypedSearchPredicateFactory<SR> extends SearchPredicateFactory {

	/**
	 * Match all documents.
	 *
	 * @return The initial step of a DSL where the "match all" predicate can be defined.
	 * @see MatchAllPredicateOptionsStep
	 */
	@Override
	MatchAllPredicateOptionsStep<SR, ?> matchAll();

	/**
	 * Match documents if they match a combination of boolean clauses.
	 *
	 * @return The initial step of a DSL where the "boolean" predicate can be defined.
	 * @see BooleanPredicateClausesStep
	 */
	@Override
	BooleanPredicateClausesStep<SR, ?> bool();

	/**
	 * Match documents if they match all inner clauses.
	 *
	 * @return The initial step of a DSL where predicates that must match can be added and options can be set.
	 * @see GenericSimpleBooleanPredicateClausesStep
	 */
	@Override
	SimpleBooleanPredicateClausesStep<SR, ?> and();

	/**
	 * Match documents if they match any inner clause.
	 *
	 * @return The initial step of a DSL where predicates that should match can be added and options can be set.
	 * @see GenericSimpleBooleanPredicateClausesStep
	 */
	@Override
	SimpleBooleanPredicateClausesStep<SR, ?> or();

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
	@Override
	MatchPredicateFieldStep<SR, ?> match();

	/**
	 * Match documents where targeted fields have a value within lower and upper bounds.
	 *
	 * @return The initial step of a DSL where the "range" predicate can be defined.
	 * @see RangePredicateFieldStep
	 */
	@Override
	RangePredicateFieldStep<SR, ?> range();

	/**
	 * Match documents where targeted fields have a value that contains a given phrase.
	 *
	 * @return The initial step of a DSL where the "phrase" predicate can be defined.
	 * @see PhrasePredicateFieldStep
	 */
	@Override
	PhrasePredicateFieldStep<SR, ?> phrase();

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
	@Override
	WildcardPredicateFieldStep<SR, ?> wildcard();

	/**
	 * Match documents where targeted fields have a value that starts with a given string.
	 *
	 * @return The initial step of a DSL where the "prefix" predicate can be defined.
	 * @see PrefixPredicateFieldStep
	 */
	@Override
	PrefixPredicateFieldStep<SR, ?> prefix();

	/**
	 * Match documents where targeted fields contain a term that matches a given regular expression.
	 *
	 * @return The initial step of a DSL where the "regexp" predicate can be defined.
	 * @see RegexpPredicateFieldStep
	 */
	@Override
	RegexpPredicateFieldStep<SR, ?> regexp();

	/**
	 * Match documents where targeted fields contain a term that matches some terms of a given series of terms.
	 *
	 * @return The initial step of a DSL where the "terms" predicate can be defined.
	 * @see TermsPredicateFieldStep
	 */
	@Override
	TermsPredicateFieldStep<SR, ?> terms();

	/**
	 * Match documents where a {@link ObjectStructure#NESTED nested object} matches a given predicate.
	 *
	 * @return The initial step of a DSL where the "nested" predicate can be defined.
	 * @see NestedPredicateFieldStep
	 * @deprecated Use {@link #nested(String)} instead.
	 */
	@Override
	@Deprecated(since = "6.2")
	NestedPredicateFieldStep<SR, ?> nested();

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
	@Override
	NestedPredicateClausesStep<SR, ?> nested(String objectFieldPath);

	/**
	 * Match documents where a {@link ObjectStructure#NESTED nested object} matches inner predicates
	 * to be defined in the next steps.
	 * <p>
	 * The resulting nested predicate must match <em>all</em> inner clauses,
	 * similarly to an {@link #and() "and" predicate}.
	 *
	 * @param fieldReference The field reference representing a <a href="SearchPredicateFactory.html#field-references">definition</a> of the object field
	 * to apply the predicate on.
	 * @return The initial step of a DSL where the "nested" predicate can be defined.
	 * @see NestedPredicateFieldStep
	 */
	default NestedPredicateClausesStep<SR, ?> nested(NestedPredicateFieldReference<? super SR> fieldReference) {
		return nested( fieldReference.absolutePath() );
	}

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
	@Override
	SimpleQueryStringPredicateFieldStep<SR, ?> simpleQueryString();

	/**
	 * Match documents according to a given query string,
	 * using the Lucene's query language.
	 * <p>
	 * Note that by default, unless the query string contains explicit operators,
	 * documents will match if <em>any</em> term mentioned in the query string is present in the document (OR operator).
	 * This makes sense when sorting results by relevance, but is not ideal otherwise.
	 * See {@link QueryStringPredicateOptionsStep#defaultOperator(BooleanOperator)} to change this behavior.
	 *
	 * @return The initial step of a DSL where the "query string" predicate can be defined.
	 * @see QueryStringPredicateFieldStep
	 */
	@Override
	QueryStringPredicateFieldStep<SR, ?> queryString();

	/**
	 * Match documents where a given field exists.
	 * <p>
	 * Fields are considered to exist in a document when they have at least one non-null value in this document.
	 *
	 * @return The initial step of a DSL where the "exists" predicate can be defined.
	 * @see ExistsPredicateFieldStep
	 */
	@Override
	ExistsPredicateFieldStep<SR, ?> exists();

	/**
	 * Access the different types of spatial predicates.
	 *
	 * @return The initial step of a DSL where spatial predicates can be defined.
	 * @see SpatialPredicateInitialStep
	 */
	@Override
	SpatialPredicateInitialStep<SR> spatial();

	/**
	 * Match {@code k} documents whose vector field value is nearest to the given vector.
	 * <p>
	 * "knn" stands for "K-Nearest Neighbors"; it is a form of vector search.
	 *
	 * @param k The number of nearest neighbors to look for.
	 * @return The initial step of a DSL where knn predicate options can be defined.
	 * @see KnnPredicateVectorStep
	 * @see KnnPredicateOptionsStep
	 */
	@Override
	KnnPredicateFieldStep<SR> knn(int k);

	/**
	 * Create a DSL step allowing multiple attempts to apply extensions one after the other,
	 * failing only if <em>none</em> of the extensions is supported.
	 * <p>
	 * If you only need to apply a single extension and fail if it is not supported,
	 * use the simpler {@link #extension(TypedSearchPredicateFactoryExtension)} method instead.
	 *
	 * @return A DSL step.
	 */
	@Override
	SearchPredicateFactoryExtensionIfSupportedStep<SR> extension();

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
	TypedSearchPredicateFactory<SR> withRoot(String objectFieldPath);

	/**
	 * @param relativeFieldPath The path to a field, relative to the {@link #withRoot(String) root} of this factory.
	 * @return The absolute path of the field, for use in native predicates for example.
	 * Note the path is returned even if the field doesn't exist.
	 */
	@Incubating
	String toAbsolutePath(String relativeFieldPath);

}
