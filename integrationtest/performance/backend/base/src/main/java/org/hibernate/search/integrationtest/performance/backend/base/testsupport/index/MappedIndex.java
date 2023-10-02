/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.index;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.analysis.Analyzers;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.openjdk.jmh.annotations.CompilerControl;

@CompilerControl(CompilerControl.Mode.INLINE)
public class MappedIndex extends StubMappedIndex {

	public static final String SHORT_TEXT_FIELD_NAME = "shortText";
	public static final String LONG_TEXT_FIELD_NAME = "longText";
	public static final String NUMERIC_FIELD_NAME = "numeric";

	private IndexFieldReference<String> shortTextField;
	private IndexFieldReference<String> longTextField;
	private IndexFieldReference<Long> numericField;

	public MappedIndex(int indexId) {
		name( "index_" + indexId );
		typeName( "type_" + indexId );
	}

	@Override
	protected void bind(IndexedEntityBindingContext context) {
		IndexSchemaElement root = context.schemaElement();
		shortTextField = root.field(
				SHORT_TEXT_FIELD_NAME,
				f -> f.asString().normalizer( Analyzers.NORMALIZER_ENGLISH ).sortable( Sortable.YES )
		)
				.toReference();
		longTextField = root.field( LONG_TEXT_FIELD_NAME, f -> f.asString().analyzer( Analyzers.ANALYZER_ENGLISH ) )
				.toReference();
		numericField = root.field( NUMERIC_FIELD_NAME, f -> f.asLong() ).toReference();
	}

	public void populate(DocumentElement documentElement, String shortText, String longText, long numeric) {
		documentElement.addValue( shortTextField, shortText );
		documentElement.addValue( longTextField, longText );
		documentElement.addValue( numericField, numeric );
	}

}
