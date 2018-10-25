/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import org.hibernate.search.engine.common.dsl.impl.DslExtensionState;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContextExtension;

public class ValueBridgeToIndexedValueContextImpl implements ValueBridgeToIndexedValueContext {

	private final MappingContextImplementor mappingContext;

	public ValueBridgeToIndexedValueContextImpl(MappingContextImplementor mappingContext) {
		this.mappingContext = mappingContext;
	}

	@Override
	public <T> T extension(ValueBridgeToIndexedValueContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension,
				extension.extendOptional( this, mappingContext )
		);
	}
}
