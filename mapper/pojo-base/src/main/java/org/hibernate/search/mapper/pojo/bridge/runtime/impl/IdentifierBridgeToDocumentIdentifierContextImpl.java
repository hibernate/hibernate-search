/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContextExtension;

public class IdentifierBridgeToDocumentIdentifierContextImpl implements IdentifierBridgeToDocumentIdentifierContext {

	private final BackendMappingContext mappingContext;

	public IdentifierBridgeToDocumentIdentifierContextImpl(BackendMappingContext mappingContext) {
		this.mappingContext = mappingContext;
	}

	@Override
	public <T> T extension(IdentifierBridgeToDocumentIdentifierContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension,
				extension.extendOptional( this, mappingContext )
		);
	}
}
