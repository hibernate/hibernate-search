/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import org.hibernate.search.engine.backend.document.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.document.converter.runtime.spi.ToDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.backend.document.converter.runtime.spi.ToDocumentIdentifierValueConvertContextImpl;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;

import com.google.gson.Gson;

public final class ElasticsearchSearchContext {

	private final ToDocumentIdentifierValueConvertContext toDocumentIdentifierValueConvertContext;

	private final ToDocumentFieldValueConvertContext toDocumentFieldValueConvertContext;

	private final Gson userFacingGson;

	public ElasticsearchSearchContext(MappingContextImplementor mappingContext, Gson userFacingGson) {
		this.toDocumentIdentifierValueConvertContext = new ToDocumentIdentifierValueConvertContextImpl( mappingContext );
		this.toDocumentFieldValueConvertContext = new ToDocumentFieldValueConvertContextImpl( mappingContext );
		this.userFacingGson = userFacingGson;
	}

	public ToDocumentIdentifierValueConvertContext getToDocumentIdentifierValueConvertContext() {
		return toDocumentIdentifierValueConvertContext;
	}

	public ToDocumentFieldValueConvertContext getToDocumentFieldValueConvertContext() {
		return toDocumentFieldValueConvertContext;
	}

	public Gson getUserFacingGson() {
		return userFacingGson;
	}
}
