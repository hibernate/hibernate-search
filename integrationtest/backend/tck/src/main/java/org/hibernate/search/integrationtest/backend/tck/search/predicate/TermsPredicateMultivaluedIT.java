/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.BooleanFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.GeoPointFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TermsPredicateMultivaluedIT<F> {

	private static final List<FieldTypeDescriptor<?>> types = new ArrayList<>();
	private static final List<TypeValues<?>> typeValuesSet = new ArrayList<>();
	private static final List<Object[]> parameters = new ArrayList<>();

	private static final String DOC_ID = "my_only_document";

	static {
		for ( FieldTypeDescriptor<?> type : FieldTypeDescriptor.getAll() ) {
			if ( GeoPointFieldTypeDescriptor.INSTANCE.equals( type ) ) {
				continue;
			}
			if ( BooleanFieldTypeDescriptor.INSTANCE.equals( type ) ) {
				continue;
			}
			types.add( type );
			TypeValues<?> typeValues = new TypeValues<>( type );
			typeValuesSet.add( typeValues );
			parameters.add( new Object[] { typeValues } );
		}
	}

	private static final SimpleMappedIndex<IndexBinding> index =
			SimpleMappedIndex.of( root -> new IndexBinding( root, types ) ).name( "simpleField" );

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndexes( index ).setup();
		BulkIndexer indexer = index.bulkIndexer();
		indexer.add( DOC_ID, doc -> {
			for ( TypeValues<?> typeValues : typeValuesSet ) {
				typeValues.contribute( doc );
			}
		} );
		indexer.join();
	}

	@Parameterized.Parameters(name = "{0}")
	public static List<Object[]> parameters() {
		return parameters;
	}

	private final TermsPredicateTestValues<F> values;

	public TermsPredicateMultivaluedIT(TypeValues<F> typeValues) {
		this.values = typeValues.values;
	}

	@Test
	public void matchingAny_rightTerms() {
		String path = index.binding().field.get( values.fieldType() ).relativeFieldName;
		for ( int i = 0; i < values.size(); i++ ) {
			F term = values.matchingArg( i );
			assertThatQuery( index.query().where( f -> f.terms().field( path ).matchingAny( term ) ) )
					.hasDocRefHitsAnyOrder( index.typeName(), DOC_ID );
		}
	}

	@Test
	public void matchingAny_wrongTerms() {
		String path = index.binding().field.get( values.fieldType() ).relativeFieldName;
		for ( int i = 0; i < values.nonMatchingArgsSize(); i++ ) {
			F term = values.nonMatchingArg( i );
			assertThatQuery( index.query().where( f -> f.terms().field( path ).matchingAny( term ) ) )
					.hasNoHits();
		}
	}

	@Test
	public void matchingAll_someTerms() {
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

	@Test
	public void matchingAll_allTerms() {
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

	@Test
	public void matchingAll_oneWrongTerm() {
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

		public IndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			field = SimpleFieldModelsByType.mapAllMultiValued( fieldTypes, root, "field0_" );
		}
	}

	public static final class TypeValues<T> {
		private final TermsPredicateTestValues<T> values;

		public TypeValues(FieldTypeDescriptor<T> type) {
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
