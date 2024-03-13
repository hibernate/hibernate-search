/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import java.util.Optional;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContextExtension;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;

final class PojoIdentifierBridgeContextExtension
		implements ToDocumentValueConvertContextExtension<IdentifierBridgeToDocumentIdentifierContext>,
		FromDocumentValueConvertContextExtension<IdentifierBridgeFromDocumentIdentifierContext> {
	static final PojoIdentifierBridgeContextExtension INSTANCE = new PojoIdentifierBridgeContextExtension();

	@Override
	public Optional<IdentifierBridgeToDocumentIdentifierContext> extendOptional(
			ToDocumentValueConvertContext original, BackendMappingContext mappingContext) {
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
			FromDocumentValueConvertContext original, BackendSessionContext sessionContext) {
		if ( sessionContext instanceof BridgeSessionContext ) {
			BridgeSessionContext pojoSessionContext = (BridgeSessionContext) sessionContext;
			return Optional.of( pojoSessionContext.identifierBridgeFromDocumentIdentifierContext() );
		}
		return Optional.empty();
	}
}
