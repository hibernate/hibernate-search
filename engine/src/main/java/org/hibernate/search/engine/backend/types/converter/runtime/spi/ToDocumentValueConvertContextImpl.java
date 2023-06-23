/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
