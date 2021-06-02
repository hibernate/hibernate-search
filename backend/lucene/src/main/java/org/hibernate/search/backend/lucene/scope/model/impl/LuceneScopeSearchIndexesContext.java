/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.scope.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.document.model.impl.AbstractLuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneMultiIndexSearchObjectFieldContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneMultiIndexSearchRootContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneMultiIndexSearchValueFieldContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchCompositeIndexSchemaElementContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexSchemaElementContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexesContext;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.StringDocumentIdentifierValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public class LuceneScopeSearchIndexesContext implements LuceneSearchIndexesContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final StringDocumentIdentifierValueConverter RAW_ID_CONVERTER =
			new StringDocumentIdentifierValueConverter();

	private final Map<String, LuceneScopeIndexManagerContext> mappedTypeNameToIndex;
	private final Set<String> indexNames;

	public LuceneScopeSearchIndexesContext(Set<? extends LuceneScopeIndexManagerContext> indexManagerContexts) {
		// Use LinkedHashMap/LinkedHashSet to ensure stable order when generating requests
		this.mappedTypeNameToIndex = new LinkedHashMap<>();
		this.indexNames = new LinkedHashSet<>();
		for ( LuceneScopeIndexManagerContext indexManager : indexManagerContexts ) {
			this.mappedTypeNameToIndex.put( indexManager.model().mappedTypeName(), indexManager );
			this.indexNames.add( indexManager.model().hibernateSearchName() );
		}
	}

	@Override
	public Collection<LuceneScopeIndexManagerContext> elements() {
		return mappedTypeNameToIndex.values();
	}

	@Override
	public Map<String, ? extends LuceneSearchIndexContext> mappedTypeNameToIndex() {
		return mappedTypeNameToIndex;
	}

	@Override
	public Set<String> hibernateSearchIndexNames() {
		return indexNames;
	}

	@Override
	public DocumentIdentifierValueConverter<?> idDslConverter(ValueConvert valueConvert) {
		if ( ValueConvert.NO.equals( valueConvert ) ) {
			return RAW_ID_CONVERTER;
		}
		DocumentIdentifierValueConverter<?> converter = null;
		for ( LuceneScopeIndexManagerContext index : elements() ) {
			DocumentIdentifierValueConverter<?> converterForIndex = index.model().idDslConverter();
			if ( converter == null ) {
				converter = converterForIndex;
			}
			else if ( !converter.isCompatibleWith( converterForIndex ) ) {
				throw log.inconsistentConfigurationForIdentifierForSearch( converter, converterForIndex, indexesEventContext() );
			}
		}
		return converter;
	}

	@Override
	public LuceneSearchCompositeIndexSchemaElementContext root() {
		if ( elements().size() == 1 ) {
			return elements().iterator().next().model().root();
		}
		else {
			List<LuceneSearchCompositeIndexSchemaElementContext> rootForEachIndex = new ArrayList<>();
			for ( LuceneScopeIndexManagerContext index : elements() ) {
				rootForEachIndex.add( index.model().root() );
			}
			return new LuceneMultiIndexSearchRootContext( this, rootForEachIndex );
		}
	}

	@Override
	public LuceneSearchIndexSchemaElementContext field(String absoluteFieldPath) {
		LuceneSearchIndexSchemaElementContext resultOrNull;
		if ( elements().size() == 1 ) {
			resultOrNull = elements().iterator().next().model().fieldOrNull( absoluteFieldPath );
		}
		else {
			resultOrNull = createMultiIndexFieldContext( absoluteFieldPath );
		}
		if ( resultOrNull == null ) {
			throw log.unknownFieldForSearch( absoluteFieldPath, indexesEventContext() );
		}
		return resultOrNull;
	}

	@Override
	public boolean hasNestedDocuments() {
		for ( LuceneScopeIndexManagerContext element : elements() ) {
			if ( element.model().hasNestedDocuments() ) {
				return true;
			}
		}
		return false;
	}

	private EventContext indexesEventContext() {
		return EventContexts.fromIndexNames( indexNames );
	}

	@SuppressWarnings({"rawtypes", "unchecked"}) // We check types using reflection
	private LuceneSearchIndexSchemaElementContext createMultiIndexFieldContext(String absoluteFieldPath) {
		List<LuceneSearchIndexSchemaElementContext> fieldForEachIndex = new ArrayList<>();
		LuceneScopeIndexManagerContext indexOfFirstField = null;
		AbstractLuceneIndexSchemaFieldNode firstField = null;

		for ( LuceneScopeIndexManagerContext index : elements() ) {
			LuceneIndexModel indexModel = index.model();
			AbstractLuceneIndexSchemaFieldNode fieldForCurrentIndex = indexModel.fieldOrNull( absoluteFieldPath );
			if ( fieldForCurrentIndex == null ) {
				continue;
			}
			if ( firstField == null ) {
				indexOfFirstField = index;
				firstField = fieldForCurrentIndex;
			}
			else if ( firstField.isObjectField() != fieldForCurrentIndex.isObjectField() ) {
				SearchException cause = log.conflictingFieldModel();
				throw log.inconsistentConfigurationForIndexElementForSearch(
						EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ), cause.getMessage(),
						EventContexts.fromIndexNames( indexOfFirstField.model().hibernateSearchName(),
								index.model().hibernateSearchName() ),
						cause );
			}
			fieldForEachIndex.add( fieldForCurrentIndex );
		}

		if ( fieldForEachIndex.isEmpty() ) {
			return null;
		}

		if ( firstField.isObjectField() ) {
			return new LuceneMultiIndexSearchObjectFieldContext( this, absoluteFieldPath,
					(List) fieldForEachIndex );
		}
		else {
			return new LuceneMultiIndexSearchValueFieldContext<>( indexNames, absoluteFieldPath,
					(List) fieldForEachIndex );
		}
	}
}
