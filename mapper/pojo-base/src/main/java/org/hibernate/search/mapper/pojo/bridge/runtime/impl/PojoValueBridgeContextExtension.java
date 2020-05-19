/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import java.util.Optional;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContextExtension;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContextExtension;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;

public final class PojoValueBridgeContextExtension
		implements ToDocumentFieldValueConvertContextExtension<ValueBridgeToIndexedValueContext>,
		FromDocumentFieldValueConvertContextExtension<ValueBridgeFromIndexedValueContext> {
	public static final PojoValueBridgeContextExtension INSTANCE = new PojoValueBridgeContextExtension();

	private PojoValueBridgeContextExtension() {
	}

	@Override
	public Optional<ValueBridgeToIndexedValueContext> extendOptional(ToDocumentFieldValueConvertContext original,
		BackendMappingContext mappingContext) {
		if ( mappingContext instanceof BridgeMappingContext ) {
			BridgeMappingContext pojoMappingContext = (BridgeMappingContext) mappingContext;
			return Optional.of( pojoMappingContext.valueBridgeToIndexedValueContext() );
		}
		else {
			return Optional.empty();
		}
	}

	@Override
	public Optional<ValueBridgeFromIndexedValueContext> extendOptional(FromDocumentFieldValueConvertContext original,
			BackendSessionContext sessionContext) {
		if ( sessionContext instanceof BridgeSessionContext ) {
			BridgeSessionContext pojoSessionContext = (BridgeSessionContext) sessionContext;
			return Optional.of( pojoSessionContext.valueBridgeFromIndexedValueContext() );
		}
		else {
			return Optional.empty();
		}
	}
}
