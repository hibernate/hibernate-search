/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContextImpl;

public class StubBackendMappingContext implements BackendMappingContext {

	private final ToDocumentIdentifierValueConvertContext toDocumentIdentifierValueConvertContext;
	private final ToDocumentFieldValueConvertContext toDocumentFieldValueConvertContext;

	public StubBackendMappingContext() {
		this.toDocumentIdentifierValueConvertContext =
				new ToDocumentIdentifierValueConvertContextImpl( this );
		this.toDocumentFieldValueConvertContext = new ToDocumentFieldValueConvertContextImpl( this );
	}

	@Override
	public final ToDocumentIdentifierValueConvertContext toDocumentIdentifierValueConvertContext() {
		return toDocumentIdentifierValueConvertContext;
	}

	@Override
	public final ToDocumentFieldValueConvertContext toDocumentFieldValueConvertContext() {
		return toDocumentFieldValueConvertContext;
	}
}
