/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import java.util.Optional;

import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContextExtension;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.mapping.context.spi.AbstractPojoBackendMappingContext;

public final class PojoIdentifierBridgeToDocumentIdentifierValueConverter<I> implements ToDocumentIdentifierValueConverter<I> {

	private final IdentifierBridge<I> bridge;
	private final Class<I> expectedValueType;

	public PojoIdentifierBridgeToDocumentIdentifierValueConverter(IdentifierBridge<I> bridge, Class<I> expectedValueType) {
		this.bridge = bridge;
		this.expectedValueType = expectedValueType;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[bridge=" + bridge + ",expectedValueType=" + expectedValueType + "]";
	}

	@Override
	public String convert(I value, ToDocumentIdentifierValueConvertContext context) {
		IdentifierBridgeToDocumentIdentifierContext extension = context.extension( PojoIdentifierBridgeContextExtension.INSTANCE );
		return bridge.toDocumentIdentifier( value, extension );
	}

	@Override
	public String convertUnknown(Object value, ToDocumentIdentifierValueConvertContext context) {
		return convert( expectedValueType.cast( value ), context );
	}

	@Override
	public boolean isCompatibleWith(ToDocumentIdentifierValueConverter<?> other) {
		if ( other == null || !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PojoIdentifierBridgeToDocumentIdentifierValueConverter<?> castedOther =
				(PojoIdentifierBridgeToDocumentIdentifierValueConverter<?>) other;
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
			if ( mappingContext instanceof AbstractPojoBackendMappingContext ) {
				AbstractPojoBackendMappingContext pojoMappingContext = (AbstractPojoBackendMappingContext) mappingContext;
				return Optional.of( pojoMappingContext.getIdentifierBridgeToDocumentIdentifierContext() );
			}
			else {
				return Optional.empty();
			}
		}
	}
}
