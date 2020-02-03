/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchScopeModel;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContextImpl;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;

import com.google.gson.Gson;

public final class ElasticsearchSearchContext {

	// Mapping context
	private final ToDocumentIdentifierValueConvertContext toDocumentIdentifierValueConvertContext;
	private final ToDocumentFieldValueConvertContext toDocumentFieldValueConvertContext;

	// Backend context
	private final Gson userFacingGson;
	private final ElasticsearchSearchSyntax searchSyntax;
	private final MultiTenancyStrategy multiTenancyStrategy;

	// Targeted indexes
	private final ElasticsearchScopeModel scopeModel;

	public ElasticsearchSearchContext(BackendMappingContext mappingContext,
			Gson userFacingGson, ElasticsearchSearchSyntax searchSyntax,
			MultiTenancyStrategy multiTenancyStrategy,
			ElasticsearchScopeModel scopeModel) {
		this.toDocumentIdentifierValueConvertContext = new ToDocumentIdentifierValueConvertContextImpl( mappingContext );
		this.toDocumentFieldValueConvertContext = new ToDocumentFieldValueConvertContextImpl( mappingContext );
		this.userFacingGson = userFacingGson;
		this.searchSyntax = searchSyntax;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.scopeModel = scopeModel;
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

	public ElasticsearchSearchSyntax getSearchSyntax() {
		return searchSyntax;
	}

	public String toElasticsearchId(String tenantId, String id) {
		return multiTenancyStrategy.toElasticsearchId( tenantId, id );
	}

	public Map<String, URLEncodedString> getHibernateSearchIndexNamesToIndexReadNames() {
		return scopeModel.getHibernateSearchIndexNamesToIndexReadNames();
	}

	public Set<String> getHibernateSearchIndexNames() {
		return scopeModel.getHibernateSearchIndexNames();
	}

	public Set<String> getMappedTypeNames() {
		return scopeModel.getMappedTypeNames();
	}
}
