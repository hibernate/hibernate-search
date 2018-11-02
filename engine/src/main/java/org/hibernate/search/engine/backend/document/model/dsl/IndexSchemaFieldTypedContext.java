/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;

import org.hibernate.search.engine.backend.document.converter.FromIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.ToIndexFieldValueConverter;

/**
 * @param <S> The concrete type of this context.
 * @param <F> The type of field values.
 */
public interface IndexSchemaFieldTypedContext<S extends IndexSchemaFieldTypedContext<? extends S, F>, F> {

	S dslConverter(ToIndexFieldValueConverter<?, ? extends F> toIndexConverter);

	S projectionConverter(FromIndexFieldValueConverter<? super F, ?> fromIndexConverter);

}
