/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.dsl;

import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.TermVector;

/**
 * A context for specifying a {@link String} type.
 *
 * @param <S> The type of this context.
 */
public interface StringIndexFieldTypeContext<S extends StringIndexFieldTypeContext<? extends S>>
		extends StandardIndexFieldTypeContext<S, String> {

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

}
