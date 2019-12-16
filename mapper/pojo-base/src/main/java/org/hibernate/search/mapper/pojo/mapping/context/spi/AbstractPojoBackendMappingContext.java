/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.context.spi;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.IdentifierBridgeToDocumentIdentifierContextImpl;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.ValueBridgeToIndexedValueContextImpl;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;

public abstract class AbstractPojoBackendMappingContext
		implements BackendMappingContext, BridgeMappingContext {

	private final IdentifierBridgeToDocumentIdentifierContext toDocumentIdentifierContext;
	private final ValueBridgeToIndexedValueContext toIndexedValueContext;

	protected AbstractPojoBackendMappingContext() {
		this.toDocumentIdentifierContext = new IdentifierBridgeToDocumentIdentifierContextImpl( this );
		this.toIndexedValueContext = new ValueBridgeToIndexedValueContextImpl( this );
	}

	@Override
	public final IdentifierBridgeToDocumentIdentifierContext getIdentifierBridgeToDocumentIdentifierContext() {
		return toDocumentIdentifierContext;
	}

	@Override
	public ValueBridgeToIndexedValueContext getValueBridgeToIndexedValueContext() {
		return toIndexedValueContext;
	}
}
