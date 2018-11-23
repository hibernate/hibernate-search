/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.converter.runtime.spi;

import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexFieldValueConvertContextExtension;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;

public class ToIndexFieldValueConvertContextImpl implements ToIndexFieldValueConvertContext {
	private final MappingContextImplementor mappingContext;

	public ToIndexFieldValueConvertContextImpl(MappingContextImplementor mappingContext) {
		this.mappingContext = mappingContext;
	}

	@Override
	public <T> T extension(ToIndexFieldValueConvertContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this, mappingContext ) );
	}
}
