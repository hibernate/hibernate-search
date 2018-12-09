/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.converter.runtime.spi;

import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexIdValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexIdValueConvertContextExtension;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;

public class ToIndexIdValueConvertContextImpl implements ToIndexIdValueConvertContext {
	private final MappingContextImplementor mappingContext;

	public ToIndexIdValueConvertContextImpl(MappingContextImplementor mappingContext) {
		this.mappingContext = mappingContext;
	}

	@Override
	public <T> T extension(ToIndexIdValueConvertContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this, mappingContext ) );
	}
}
