/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.elasticsearch.common.impl.DocumentIdHelper;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public interface ElasticsearchSearchIndexScope {

	ToDocumentIdentifierValueConvertContext toDocumentIdentifierValueConvertContext();

	ToDocumentFieldValueConvertContext toDocumentFieldValueConvertContext();

	Gson userFacingGson();

	ElasticsearchSearchSyntax searchSyntax();

	DocumentIdHelper documentIdHelper();

	JsonObject filterOrNull(String tenantId);

	TimeoutManager createTimeoutManager(Long timeout, TimeUnit timeUnit, boolean exceptionOnTimeout);

	Collection<ElasticsearchSearchIndexContext> indexes();

	Set<String> hibernateSearchIndexNames();

	Map<String, ElasticsearchSearchIndexContext> mappedTypeNameToIndex();

	DocumentIdentifierValueConverter<?> idDslConverter(ValueConvert valueConvert);

	ElasticsearchSearchCompositeIndexSchemaElementContext root();

	ElasticsearchSearchIndexSchemaElementContext field(String absoluteFieldPath);

	int maxResultWindow();
}
