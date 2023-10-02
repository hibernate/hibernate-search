/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.converter.spi;

import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;

final class PassThroughToDocumentValueConverter<F> implements ToDocumentValueConverter<F, F> {

	@Override
	public F toDocumentValue(F value, ToDocumentValueConvertContext context) {
		return value;
	}

	@Override
	public boolean isCompatibleWith(ToDocumentValueConverter<?, ?> other) {
		return getClass().equals( other.getClass() );
	}
}
