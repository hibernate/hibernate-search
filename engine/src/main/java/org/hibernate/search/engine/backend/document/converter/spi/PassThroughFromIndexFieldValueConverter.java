/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.converter.spi;

import org.hibernate.search.engine.backend.document.converter.FromIndexFieldValueConverter;

public final class PassThroughFromIndexFieldValueConverter<F> implements FromIndexFieldValueConverter<F, F> {

	private static final PassThroughFromIndexFieldValueConverter<Object> INSTANCE = new PassThroughFromIndexFieldValueConverter<>();

	@SuppressWarnings("unchecked") // INSTANCE works for any F
	public static <F> PassThroughFromIndexFieldValueConverter<F> get() {
		return (PassThroughFromIndexFieldValueConverter<F>) INSTANCE;
	}

	private PassThroughFromIndexFieldValueConverter() {
		// private, use get() instead
	}

	@Override
	public F convert(F value) {
		return value;
	}

}
