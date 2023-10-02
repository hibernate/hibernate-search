/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.converter.runtime.spi;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContextExtension;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;

public class ToDocumentValueConvertContextImpl implements ToDocumentValueConvertContext {
	private final BackendMappingContext mappingContext;

	public ToDocumentValueConvertContextImpl(BackendMappingContext mappingContext) {
		this.mappingContext = mappingContext;
	}

	@Override
	@Deprecated
	public <T> T extension(
			org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContextExtension<
					T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this, mappingContext ) );
	}

	@Override
	public <T> T extension(ToDocumentValueConvertContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this, mappingContext ) );
	}
}
