/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.dsl;

import org.hibernate.search.engine.backend.types.Norms;

/**
 * @param <S> The type of this context.
 */
public interface StringIndexFieldTypeContext<S extends StringIndexFieldTypeContext<? extends S>>
		extends StandardIndexFieldTypeContext<S, String> {

	S analyzer(String analyzerName);

	S normalizer(String normalizerName);

	S norms(Norms norms);

}
