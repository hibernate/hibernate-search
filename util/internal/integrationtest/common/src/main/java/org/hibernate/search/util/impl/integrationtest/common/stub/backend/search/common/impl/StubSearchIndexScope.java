/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexField;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;

public class StubSearchIndexScope implements SearchIndexScope<StubSearchIndexScope> {
	private final Set<String> indexNames;
	private final Set<StubIndexModel> indexModels;

	public StubSearchIndexScope(Set<StubIndexModel> indexModels) {
		Set<String> mutableIndexNames = indexModels.stream().map( StubIndexModel::hibernateSearchName )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
		this.indexNames = Collections.unmodifiableSet( mutableIndexNames );
		this.indexModels = indexModels;
	}

	@Override
	public Set<String> hibernateSearchIndexNames() {
		return indexNames;
	}

	public DocumentIdentifierValueConverter<?> idDslConverter() {
		for ( StubIndexModel model : indexModels ) {
			DocumentIdentifierValueConverter<?> converter = model.idDslConverter();

			// there is no need to check the compatibility - this is a stub backend
			if ( converter != null ) {
				return converter;
			}
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public StubSearchIndexCompositeNodeContext root() {
		if ( indexModels.size() == 1 ) {
			return indexModels.iterator().next().root();
		}
		else {
			List<StubSearchIndexNodeContext> fieldForEachIndex = new ArrayList<>();
			for ( StubIndexModel model : indexModels ) {
				fieldForEachIndex.add( model.root() );
			}
			return new StubMultiIndexSearchIndexCompositeNodeContext( this, null, (List) fieldForEachIndex );
		}
	}

	@Override
	public StubSearchIndexNodeContext field(String absoluteFieldPath) {
		StubSearchIndexNodeContext resultOrNull;
		if ( indexModels.size() == 1 ) {
			resultOrNull = indexModels.iterator().next().fieldOrNull( absoluteFieldPath );
		}
		else {
			resultOrNull = createMultiIndexFieldContext( absoluteFieldPath );
		}
		if ( resultOrNull == null ) {
			throw new SearchException( "Unknown field: " + absoluteFieldPath );
		}
		return resultOrNull;
	}

	@SuppressWarnings("unchecked")
	private StubSearchIndexNodeContext createMultiIndexFieldContext(String absoluteFieldPath) {
		List<StubSearchIndexNodeContext> fieldForEachIndex = new ArrayList<>();
		StubSearchIndexNodeContext firstField = null;

		for ( StubIndexModel model : indexModels ) {
			StubIndexField fieldForCurrentIndex = model.fieldOrNull( absoluteFieldPath );
			if ( fieldForCurrentIndex == null ) {
				continue;
			}
			if ( firstField == null ) {
				firstField = fieldForCurrentIndex;
			}
			else if ( firstField.isValueField() != fieldForCurrentIndex.isValueField() ) {
				throw new IllegalStateException( "Inconsistent field configuration for indexes " + indexNames
						+ "; field '" + absoluteFieldPath + "' is an object field for some indexes only." );
			}
			fieldForEachIndex.add( fieldForCurrentIndex );
		}

		if ( fieldForEachIndex.isEmpty() ) {
			return null;
		}

		if ( fieldForEachIndex.get( 0 ).isValueField() ) {
			return new StubMultiIndexSearchIndexValueFieldContext<>( this, absoluteFieldPath,
					(List) fieldForEachIndex );
		}
		else {
			return new StubMultiIndexSearchIndexCompositeNodeContext( this, absoluteFieldPath,
					(List) fieldForEachIndex );
		}
	}

}
