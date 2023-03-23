/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.dsl;

import java.util.Collection;

import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.TermVector;

/**
 * The initial and final step in a "string" index field type definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface StringIndexFieldTypeOptionsStep<S extends StringIndexFieldTypeOptionsStep<?>>
		extends StandardIndexFieldTypeOptionsStep<S, String> {

	/**
	 * Define the type as analyzed.
	 * <p>
	 * Incompatible with {@link #normalizer(String)}.
	 *
	 * @param analyzerName The name of an analyzer to apply to values before indexing and when querying the index.
	 * See the reference documentation for more information about analyzers and how to define them.
	 * @return {@code this}, for method chaining.
	 */
	S analyzer(String analyzerName);

	/**
	 * Overrides {@link #analyzer} to use for query parameters at search time.
	 * <p>
	 * A search analyzer can only be set if an analyzer was set through {@link #analyzer(String)}.
	 *
	 * @param searchAnalyzerName The name of an analyzer to apply to values when querying the index only.
	 * It overrides the {@link #analyzer(String)} when querying the index.
	 * See the reference documentation for more information about analyzers and how to define them.
	 * @return {@code this}, for method chaining.
	 */
	S searchAnalyzer(String searchAnalyzerName);

	/**
	 * Define the type as normalized.
	 * <p>
	 * Incompatible with {@link #analyzer(String)}.
	 *
	 * @param normalizerName The name of a normalizer to apply to values before indexing and when querying the index.
	 * See the reference documentation for more information about normalizers and how to define them.
	 * @return {@code this}, for method chaining.
	 */
	S normalizer(String normalizerName);

	/**
	 * @param norms Whether index-time scoring information for the field should be stored or not.
	 * @return {@code this}, for method chaining.
	 * @see Norms
	 */
	S norms(Norms norms);

	/**
	 * @param termVector The term vector storing strategy.
	 * @return {@code this}, for method chaining.
	 * @see TermVector
	 */
	S termVector(TermVector termVector);

	/**
	 * @param highlightable Whether highlighting is supported and if so which highlighter types can be applied.
	 * Pass {@code Collections.singleton(Highlightable.NO)} to disable highlighting.
	 * @return {@code this}, for method chaining.
	 * @see Highlightable
	 */
	S highlightable(Collection<Highlightable> highlightable);
}
