/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.context.spi;

import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.IdentifierBridgeToDocumentIdentifierContextImpl;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.ValueBridgeToIndexedValueContextImpl;

public abstract class AbstractPojoMappingContextImplementor implements MappingContextImplementor {

	private final IdentifierBridgeToDocumentIdentifierContext toDocumentIdentifierContext;
	private final ValueBridgeToIndexedValueContext toIndexedValueContext;

	protected AbstractPojoMappingContextImplementor() {
		this.toDocumentIdentifierContext = new IdentifierBridgeToDocumentIdentifierContextImpl( this );
		this.toIndexedValueContext = new ValueBridgeToIndexedValueContextImpl( this );
	}

	public final IdentifierBridgeToDocumentIdentifierContext getIdentifierBridgeToDocumentIdentifierContext() {
		return toDocumentIdentifierContext;
	}

	public ValueBridgeToIndexedValueContext getToIndexedValueContext() {
		return toIndexedValueContext;
	}
}
