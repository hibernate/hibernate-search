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
import org.hibernate.search.backend.lucene.search.impl.LuceneMultiIndexSearchValueFieldContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexesContext;
import org.hibernate.search.engine.backend.types.converter.spi.StringToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public class LuceneScopeSearchIndexesContext implements LuceneSearchIndexesContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final StringToDocumentIdentifierValueConverter RAW_ID_CONVERTER =
			new StringToDocumentIdentifierValueConverter();

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
	public Set<String> indexNames() {
		return indexNames;
	}

	@Override
	public ToDocumentIdentifierValueConverter<?> idDslConverter(ValueConvert valueConvert) {
		if ( ValueConvert.NO.equals( valueConvert ) ) {
			return RAW_ID_CONVERTER;
		}
		ToDocumentIdentifierValueConverter<?> converter = null;
		for ( LuceneScopeIndexManagerContext index : elements() ) {
			ToDocumentIdentifierValueConverter<?> converterForIndex = index.model().idDslConverter();
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
	@SuppressWarnings("unchecked") // We check types using reflection (see calls to type().valueType())
	public LuceneSearchFieldContext field(String absoluteFieldPath) {
		LuceneSearchFieldContext resultOrNull = null;
		if ( elements().size() == 1 ) {
			// Single-index search
			resultOrNull = elements().iterator().next().model().fieldOrNull( absoluteFieldPath );
		}
		else {
			// Multi-index search
			List<LuceneSearchFieldContext> fieldForEachIndex = new ArrayList<>();
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
				else {
					if ( firstField.isObjectField() != fieldForCurrentIndex.isObjectField() ) {
						SearchException cause = log.conflictingFieldModel();
						throw log.inconsistentConfigurationForFieldForSearch( absoluteFieldPath, cause.getMessage(),
								EventContexts.fromIndexNames( indexOfFirstField.model().hibernateSearchName(),
										index.model().hibernateSearchName() ),
								cause );
					}
				}
				fieldForEachIndex.add( fieldForCurrentIndex );
			}

			if ( !fieldForEachIndex.isEmpty() ) {
				if ( firstField.isObjectField() ) {
					resultOrNull = new LuceneMultiIndexSearchObjectFieldContext(
							this, absoluteFieldPath, (List) fieldForEachIndex
					);
				}
				else {
					resultOrNull = new LuceneMultiIndexSearchValueFieldContext<>(
							indexNames, absoluteFieldPath, (List) fieldForEachIndex
					);
				}
			}
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
}
