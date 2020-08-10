/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.elasticsearch.common.impl.DocumentIdHelper;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.search.timeout.impl.ElasticsearchTimeoutManager;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContextImpl;
import org.hibernate.search.engine.common.timing.spi.TimingSource;

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
	private final TimingSource timingSource;

	// Targeted indexes
	private final ElasticsearchSearchIndexesContext indexes;

	public ElasticsearchSearchContext(BackendMappingContext mappingContext,
			Gson userFacingGson, ElasticsearchSearchSyntax searchSyntax,
			MultiTenancyStrategy multiTenancyStrategy,
			ElasticsearchSearchIndexesContext indexes,
			TimingSource timingSource) {
		this.toDocumentIdentifierValueConvertContext = new ToDocumentIdentifierValueConvertContextImpl(
				mappingContext );
		this.toDocumentFieldValueConvertContext = new ToDocumentFieldValueConvertContextImpl( mappingContext );
		this.userFacingGson = userFacingGson;
		this.searchSyntax = searchSyntax;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.timingSource = timingSource;
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

	public ElasticsearchTimeoutManager createTimeoutManager(JsonObject definitiveQuery, Long timeout,
			TimeUnit timeUnit, boolean exceptionOnTimeout) {
		if ( timeout != null && timeUnit != null ) {
			if ( exceptionOnTimeout ) {
				return ElasticsearchTimeoutManager.hardTimeout( timingSource, definitiveQuery, timeout, timeUnit );
			}
			else {
				return ElasticsearchTimeoutManager.softTimeout( timingSource, definitiveQuery, timeout, timeUnit );
			}
		}
		return ElasticsearchTimeoutManager.noTimeout( timingSource, definitiveQuery );
	}
}
