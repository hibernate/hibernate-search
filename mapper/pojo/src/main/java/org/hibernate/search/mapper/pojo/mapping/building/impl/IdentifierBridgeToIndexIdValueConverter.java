/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.util.Optional;

import org.hibernate.search.engine.backend.document.converter.ToIndexIdValueConverter;
import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexIdValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.ToIndexIdValueConvertContextExtension;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.mapping.context.spi.AbstractPojoMappingContextImplementor;

final class IdentifierBridgeToIndexIdValueConverter<I, V extends I> implements ToIndexIdValueConverter<I> {

	private final IdentifierBridge<I> bridge;

	IdentifierBridgeToIndexIdValueConverter(IdentifierBridge<I> bridge) {
		this.bridge = bridge;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + bridge + "]";
	}

	@Override
	public String convert(I value, ToIndexIdValueConvertContext context) {
		IdentifierBridgeToDocumentIdentifierContext extension = context.extension( ToIdentifierBridgeContextExtension.INSTANCE );
		return bridge.toDocumentIdentifier( value, extension );
	}

	@Override
	public boolean isCompatibleWith(ToIndexIdValueConverter<?> other) {
		if ( other == null || !getClass().equals( other.getClass() ) ) {
			return false;
		}
		IdentifierBridgeToIndexIdValueConverter<?, ?> castedOther =
				(IdentifierBridgeToIndexIdValueConverter<?, ?>) other;
		return bridge.isCompatibleWith( castedOther.bridge );
	}

	private static class ToIdentifierBridgeContextExtension
			implements ToIndexIdValueConvertContextExtension<IdentifierBridgeToDocumentIdentifierContext> {
		private static final ToIdentifierBridgeContextExtension INSTANCE = new ToIdentifierBridgeContextExtension();

		@Override
		public Optional<IdentifierBridgeToDocumentIdentifierContext> extendOptional(ToIndexIdValueConvertContext original,
			MappingContextImplementor mappingContext) {
			if ( mappingContext instanceof AbstractPojoMappingContextImplementor ) {
				AbstractPojoMappingContextImplementor pojoMappingContext = (AbstractPojoMappingContextImplementor) mappingContext;
				return Optional.of( pojoMappingContext.getIdentifierBridgeToDocumentIdentifierContext() );
			}
			else {
				return Optional.empty();
			}
		}
	}
}
