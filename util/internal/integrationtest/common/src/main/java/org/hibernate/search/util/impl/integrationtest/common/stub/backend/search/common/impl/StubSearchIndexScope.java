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
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexNode;

public class StubSearchIndexScope {
	private final Set<String> indexNames;
	private final Set<StubIndexModel> indexModels;

	public StubSearchIndexScope(Set<StubIndexModel> indexModels) {
		Set<String> mutableIndexNames = indexModels.stream().map( StubIndexModel::hibernateSearchIndexName )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
		this.indexNames = Collections.unmodifiableSet( mutableIndexNames );
		this.indexModels = indexModels;
	}

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

	@SuppressWarnings("unchecked")
	public StubSearchIndexNodeContext field(String absoluteFieldPath) {
		List<StubSearchIndexNodeContext> fieldForEachIndex = new ArrayList<>();
		StubSearchIndexNodeContext firstField = null;

		for ( StubIndexModel model : indexModels ) {
			StubIndexNode nodeForCurrentIndex = model.fieldOrNull( absoluteFieldPath );
			if ( nodeForCurrentIndex == null ) {
				continue;
			}
			StubSearchIndexNodeContext fieldForCurrentIndex = nodeForCurrentIndex.toSearchContext();
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
