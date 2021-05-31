/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentIdentifierValueConvertContextExtension;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContextExtension;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;
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
		IdentifierBridgeToDocumentIdentifierContext extension = context.extension( ContextExtension.INSTANCE );
		return bridge.toDocumentIdentifier( value, extension );
	}

	@Override
	public String convertToDocumentUnknown(Object value, ToDocumentIdentifierValueConvertContext context) {
		return convertToDocument( expectedValueType.cast( value ), context );
	}

	@Override
	public void checkSourceTypeAssignableTo(Class<?> requiredType) {
		if ( !requiredType.isAssignableFrom( expectedValueType ) ) {
			throw log.wrongRequiredIdentifierType( requiredType, expectedValueType );
		}
	}

	@Override
	public I convertToSource(String documentId, FromDocumentIdentifierValueConvertContext context) {
		IdentifierBridgeFromDocumentIdentifierContext identifierContext =
				context.extension( ContextExtension.INSTANCE );

		return bridge.fromDocumentIdentifier( documentId, identifierContext );
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

	private static class ContextExtension
			implements ToDocumentIdentifierValueConvertContextExtension<IdentifierBridgeToDocumentIdentifierContext>,
					FromDocumentIdentifierValueConvertContextExtension<IdentifierBridgeFromDocumentIdentifierContext> {
		private static final ContextExtension INSTANCE = new ContextExtension();

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

		@Override
		public Optional<IdentifierBridgeFromDocumentIdentifierContext> extendOptional(
				FromDocumentIdentifierValueConvertContext original,
				BackendSessionContext sessionContext) {
			if ( sessionContext instanceof BridgeSessionContext ) {
				BridgeSessionContext pojoSessionContext = (BridgeSessionContext) sessionContext;
				return Optional.of( pojoSessionContext.identifierBridgeFromDocumentIdentifierContext() );
			}
			return Optional.empty();
		}
	}
}
