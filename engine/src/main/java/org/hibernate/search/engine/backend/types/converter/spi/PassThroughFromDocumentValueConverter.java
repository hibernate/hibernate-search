/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.converter.spi;

import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;

final class PassThroughFromDocumentValueConverter<F> implements FromDocumentValueConverter<F, F> {

	@Override
	public F fromDocumentValue(F value, FromDocumentValueConvertContext context) {
		return value;
	}

	@Override
	public boolean isCompatibleWith(FromDocumentValueConverter<?, ?> other) {
		return getClass().equals( other.getClass() );
	}
}
