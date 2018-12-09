/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.converter.spi;

import org.hibernate.search.engine.backend.document.converter.runtime.spi.ToIndexIdValueConvertContext;

public final class StringToIndexIdValueConverter implements ToIndexIdValueConverter<String> {

	@Override
	public String convert(String value, ToIndexIdValueConvertContext context) {
		return value;
	}

	@Override
	public String convertUnknown(Object value, ToIndexIdValueConvertContext context) {
		return (String) value;
	}

	@Override
	public boolean isCompatibleWith(ToIndexIdValueConverter<?> other) {
		return getClass().equals( other.getClass() );
	}
}
