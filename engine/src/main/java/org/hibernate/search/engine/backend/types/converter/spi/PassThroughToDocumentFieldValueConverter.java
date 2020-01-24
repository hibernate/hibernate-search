/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.converter.spi;

import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;

public final class PassThroughToDocumentFieldValueConverter<F> implements ToDocumentFieldValueConverter<F, F> {

	@Override
	public F convert(F value, ToDocumentFieldValueConvertContext context) {
		return value;
	}

	@Override
	public boolean isCompatibleWith(ToDocumentFieldValueConverter<?, ?> other) {
		return getClass().equals( other.getClass() );
	}
}
