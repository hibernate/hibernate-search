/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.common.impl.DocumentIdHelper;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContextImpl;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public final class ElasticsearchSearchContext {

	// Mapping context
	private final ToDocumentIdentifierValueConvertContext toDocumentIdentifierValueConvertContext;
	private final ToDocumentFieldValueConvertContext toDocumentFieldValueConvertContext;

	// Backend context
	private final Gson userFacingGson;
	private final ElasticsearchSearchSyntax searchSyntax;
	private final MultiTenancyStrategy multiTenancyStrategy;

	// Targeted indexes
	private final ElasticsearchSearchIndexesContext indexes;

	public ElasticsearchSearchContext(BackendMappingContext mappingContext,
			Gson userFacingGson, ElasticsearchSearchSyntax searchSyntax,
			MultiTenancyStrategy multiTenancyStrategy,
			ElasticsearchSearchIndexesContext indexes) {
		this.toDocumentIdentifierValueConvertContext = new ToDocumentIdentifierValueConvertContextImpl( mappingContext );
		this.toDocumentFieldValueConvertContext = new ToDocumentFieldValueConvertContextImpl( mappingContext );
		this.userFacingGson = userFacingGson;
		this.searchSyntax = searchSyntax;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.indexes = indexes;
	}

	public ToDocumentIdentifierValueConvertContext toDocumentIdentifierValueConvertContext() {
		return toDocumentIdentifierValueConvertContext;
	}

	public ToDocumentFieldValueConvertContext toDocumentFieldValueConvertContext() {
		return toDocumentFieldValueConvertContext;
	}

	public Gson userFacingGson() {
		return userFacingGson;
	}

	public ElasticsearchSearchSyntax searchSyntax() {
		return searchSyntax;
	}

	public DocumentIdHelper documentIdHelper() {
		return multiTenancyStrategy.documentIdHelper();
	}

	public ElasticsearchSearchIndexesContext indexes() {
		return indexes;
	}

	public JsonObject filterOrNull(String tenantId) {
		return multiTenancyStrategy.filterOrNull( tenantId );
	}
}
