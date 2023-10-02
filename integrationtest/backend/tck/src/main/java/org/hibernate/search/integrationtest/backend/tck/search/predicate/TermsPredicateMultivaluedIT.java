/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.BooleanFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TermsPredicateMultivaluedIT<F> {

	private static final List<StandardFieldTypeDescriptor<?>> types = new ArrayList<>();
	private static final List<TypeValues<?>> typeValuesSet = new ArrayList<>();
	private static final List<Arguments> parameters = new ArrayList<>();

	private static final String DOC_ID = "my_only_document";

	static {
		for ( StandardFieldTypeDescriptor<?> type : FieldTypeDescriptor.getAllStandard() ) {
			if ( GeoPointFieldTypeDescriptor.INSTANCE.equals( type ) ) {
				continue;
			}
			if ( BooleanFieldTypeDescriptor.INSTANCE.equals( type ) ) {
				continue;
			}
			types.add( type );
			TypeValues<?> typeValues = new TypeValues<>( type );
			typeValuesSet.add( typeValues );
			parameters.add( Arguments.of( typeValues.values ) );
		}
	}

	private static final SimpleMappedIndex<IndexBinding> index =
			SimpleMappedIndex.of( root -> new IndexBinding( root, types ) ).name( "simpleField" );

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndexes( index ).setup();
		BulkIndexer indexer = index.bulkIndexer();
		indexer.add( DOC_ID, doc -> {
			for ( TypeValues<?> typeValues : typeValuesSet ) {
				typeValues.contribute( doc );
			}
		} );
		indexer.join();
	}

	public static List<? extends Arguments> params() {
		return parameters;
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void matchingAny_rightTerms(TermsPredicateTestValues<F> values) {
		String path = index.binding().field.get( values.fieldType() ).relativeFieldName;
		for ( int i = 0; i < values.size(); i++ ) {
			F term = values.matchingArg( i );
			assertThatQuery( index.query().where( f -> f.terms().field( path ).matchingAny( term ) ) )
					.hasDocRefHitsAnyOrder( index.typeName(), DOC_ID );
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void matchingAny_wrongTerms(TermsPredicateTestValues<F> values) {
		String path = index.binding().field.get( values.fieldType() ).relativeFieldName;
		for ( int i = 0; i < values.nonMatchingArgsSize(); i++ ) {
			F term = values.nonMatchingArg( i );
			assertThatQuery( index.query().where( f -> f.terms().field( path ).matchingAny( term ) ) )
					.hasNoHits();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void matchingAll_someTerms(TermsPredicateTestValues<F> values) {
		String path = index.binding().field.get( values.fieldType() ).relativeFieldName;

		F firstTerm = null;
		F[] otherTerms = values.createArray( values.size() - 2 );
		for ( int i = 0; i < values.size() - 1; i++ ) {
			if ( i == 0 ) {
				firstTerm = values.matchingArg( i );
			}
			else {
				otherTerms[i - 1] = values.matchingArg( i );
			}
		}

		F finalFirstTerm = firstTerm;
		assertThatQuery( index.query().where( f -> f.terms().field( path ).matchingAll( finalFirstTerm, otherTerms ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOC_ID );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void matchingAll_allTerms(TermsPredicateTestValues<F> values) {
		String path = index.binding().field.get( values.fieldType() ).relativeFieldName;

		F firstTerm = null;
		F[] otherTerms = values.createArray( values.size() - 1 );
		for ( int i = 0; i < values.size(); i++ ) {
			if ( i == 0 ) {
				firstTerm = values.matchingArg( i );
			}
			else {
				otherTerms[i - 1] = values.matchingArg( i );
			}
		}

		F finalFirstTerm = firstTerm;
		assertThatQuery( index.query().where( f -> f.terms().field( path ).matchingAll( finalFirstTerm, otherTerms ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOC_ID );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void matchingAll_oneWrongTerm(TermsPredicateTestValues<F> values) {
		String path = index.binding().field.get( values.fieldType() ).relativeFieldName;

		F firstTerm = null;
		F[] otherTerms = values.createArray( values.size() );
		for ( int i = 0; i < values.size(); i++ ) {
			if ( i == 0 ) {
				firstTerm = values.matchingArg( i );
			}
			else {
				otherTerms[i - 1] = values.matchingArg( i );
			}
		}
		otherTerms[values.size() - 1] = values.nonMatchingArg( 0 ); // put a non-matching term

		F finalFirstTerm = firstTerm;
		assertThatQuery( index.query().where( f -> f.terms().field( path ).matchingAll( finalFirstTerm, otherTerms ) ) )
				.hasNoHits();
	}

	public static final class IndexBinding {
		private final SimpleFieldModelsByType field;

		public IndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAllMultiValued( fieldTypes, root, "field0_" );
		}
	}

	public static final class TypeValues<T> {
		private final TermsPredicateTestValues<T> values;

		public TypeValues(FieldTypeDescriptor<T, ?> type) {
			this.values = new TermsPredicateTestValues<>( type );
		}

		public void contribute(DocumentElement doc) {
			IndexFieldReference<T> reference = index.binding().field.get( values.fieldType() ).reference;
			for ( int j = 0; j < values.size(); j++ ) {
				T value = values.fieldValue( j );
				// values are added all to the same documents
				doc.addValue( reference, value );
			}
		}

		@Override
		public String toString() {
			return values.fieldType().toString();
		}
	}
}
