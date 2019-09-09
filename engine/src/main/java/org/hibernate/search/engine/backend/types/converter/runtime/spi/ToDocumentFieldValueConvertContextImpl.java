/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.converter.runtime.spi;

import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContextExtension;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;

public class ToDocumentFieldValueConvertContextImpl implements ToDocumentFieldValueConvertContext {
	private final BackendMappingContext mappingContext;

	public ToDocumentFieldValueConvertContextImpl(BackendMappingContext mappingContext) {
		this.mappingContext = mappingContext;
	}

	@Override
	public <T> T extension(ToDocumentFieldValueConvertContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this, mappingContext ) );
	}
}
