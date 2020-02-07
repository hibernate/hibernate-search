/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.index;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.analysis.Analyzers;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;

import org.openjdk.jmh.annotations.CompilerControl;

@CompilerControl(CompilerControl.Mode.INLINE)
public class MappedIndex {

	public static final String SHORT_TEXT_FIELD_NAME = "shortText";
	public static final String LONG_TEXT_FIELD_NAME = "longText";
	public static final String NUMERIC_FIELD_NAME = "numeric";

	private IndexFieldReference<String> shortTextField;
	private IndexFieldReference<String> longTextField;
	private IndexFieldReference<Long> numericField;

	private StubMappingIndexManager indexManager;

	public void bind(IndexedEntityBindingContext context) {
		IndexSchemaElement root = context.getSchemaElement();
		shortTextField = root.field(
				SHORT_TEXT_FIELD_NAME,
				f -> f.asString().normalizer( Analyzers.NORMALIZER_ENGLISH ).sortable( Sortable.YES )
		)
				.toReference();
		longTextField = root.field( LONG_TEXT_FIELD_NAME, f -> f.asString().analyzer( Analyzers.ANALYZER_ENGLISH ) )
				.toReference();
		numericField = root.field( NUMERIC_FIELD_NAME, f -> f.asLong() ).toReference();
	}

	public void setIndexManager(StubMappingIndexManager indexManager) {
		this.indexManager = indexManager;
	}

	public StubMappingIndexManager getIndexManager() {
		return indexManager;
	}

	public void populate(DocumentElement documentElement, String shortText, String longText, long numeric) {
		documentElement.addValue( shortTextField, shortText );
		documentElement.addValue( longTextField, longText );
		documentElement.addValue( numericField, numeric );
	}

}
