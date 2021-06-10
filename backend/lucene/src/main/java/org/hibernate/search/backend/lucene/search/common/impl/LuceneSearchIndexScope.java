/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.common.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentIdentifierValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

import org.apache.lucene.search.Query;

public interface LuceneSearchIndexScope extends SearchIndexScope<LuceneSearchIndexScope> {

	ToDocumentIdentifierValueConvertContext toDocumentIdentifierValueConvertContext();

	ToDocumentFieldValueConvertContext toDocumentFieldValueConvertContext();

	LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry();

	Query filterOrNull(String tenantId);

	TimeoutManager createTimeoutManager(Long timeout, TimeUnit timeUnit, boolean exceptionOnTimeout);

	Collection<? extends LuceneSearchIndexContext> indexes();

	Map<String, ? extends LuceneSearchIndexContext> mappedTypeNameToIndex();

	DocumentIdentifierValueConverter<?> idDslConverter(ValueConvert valueConvert);

	@Override
	LuceneSearchIndexCompositeNodeContext root();

	@Override
	LuceneSearchIndexNodeContext field(String absoluteFieldPath);

	boolean hasNestedDocuments();

}
