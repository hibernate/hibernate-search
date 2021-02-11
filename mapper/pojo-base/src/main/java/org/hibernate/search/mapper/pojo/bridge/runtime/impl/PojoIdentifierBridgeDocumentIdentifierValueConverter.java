/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContextExtension;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class PojoIdentifierBridgeDocumentIdentifierValueConverter<I> implements DocumentIdentifierValueConverter<I> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IdentifierBridge<I> bridge;
	private final Class<I> expectedValueType;

	public PojoIdentifierBridgeDocumentIdentifierValueConverter(IdentifierBridge<I> bridge, Class<I> expectedValueType) {
		this.bridge = bridge;
		this.expectedValueType = expectedValueType;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[bridge=" + bridge + ",expectedValueType=" + expectedValueType + "]";
	}

	@Override
	public String convertToDocument(I value, ToDocumentIdentifierValueConvertContext context) {
		IdentifierBridgeToDocumentIdentifierContext extension = context.extension( PojoIdentifierBridgeContextExtension.INSTANCE );
		return bridge.toDocumentIdentifier( value, extension );
	}

	@Override
	public String convertToDocumentUnknown(Object value, ToDocumentIdentifierValueConvertContext context) {
		return convertToDocument( expectedValueType.cast( value ), context );
	}

	@Override
	public void requiresType(Class<?> requiredType) {
		if ( !expectedValueType.isAssignableFrom( requiredType ) ) {
			throw log.wrongRequiredIdentifierType( requiredType, expectedValueType );
		}
	}

	@Override
	public boolean isCompatibleWith(DocumentIdentifierValueConverter<?> other) {
		if ( other == null || !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PojoIdentifierBridgeDocumentIdentifierValueConverter<?> castedOther =
				(PojoIdentifierBridgeDocumentIdentifierValueConverter<?>) other;
		return expectedValueType.equals( castedOther.expectedValueType )
				&& bridge.isCompatibleWith( castedOther.bridge );
	}

	private static class PojoIdentifierBridgeContextExtension
			implements ToDocumentIdentifierValueConvertContextExtension<IdentifierBridgeToDocumentIdentifierContext> {
		private static final PojoIdentifierBridgeContextExtension INSTANCE = new PojoIdentifierBridgeContextExtension();

		@Override
		public Optional<IdentifierBridgeToDocumentIdentifierContext> extendOptional(
				ToDocumentIdentifierValueConvertContext original,
				BackendMappingContext mappingContext) {
			if ( mappingContext instanceof BridgeMappingContext ) {
				BridgeMappingContext pojoMappingContext = (BridgeMappingContext) mappingContext;
				return Optional.of( pojoMappingContext.identifierBridgeToDocumentIdentifierContext() );
			}
			else {
				return Optional.empty();
			}
		}
	}
}
