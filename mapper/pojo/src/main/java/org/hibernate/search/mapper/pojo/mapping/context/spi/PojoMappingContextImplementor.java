/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.context.spi;

import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.IdentifierBridgeToDocumentIdentifierContextImpl;

public abstract class PojoMappingContextImplementor implements MappingContextImplementor {

	private final IdentifierBridgeToDocumentIdentifierContext toDocumentIdentifierContext;

	protected PojoMappingContextImplementor() {
		this.toDocumentIdentifierContext = new IdentifierBridgeToDocumentIdentifierContextImpl( this );
	}

	public final IdentifierBridgeToDocumentIdentifierContext getIdentifierBridgeToDocumentIdentifierContext() {
		return toDocumentIdentifierContext;
	}
}
