/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinitionContext;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class NamedPredicateBaseIT {

	private static final String DOCUMENT_1 = "document1";
	private static final String DOCUMENT_2 = "document2";
	private static final String EMPTY = "empty";

	private static final String WORD_1 = "word1";
	private static final String WORD_2 = "word2";
	private static final String WORD_3 = "word3";
	private static final String WORD_4 = "word4";
	private static final String WORD_5 = "word5";

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private static final DataSet dataSet = new DataSet();

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		BulkIndexer indexer = index.bulkIndexer();
		dataSet.contribute( indexer );
		indexer.join();
	}

	@Test
	public void root() {
		assertThatQuery( index.query()
				.where( f -> f.named( "match-both-fields" )
						.param( "value1", WORD_1 )
						.param( "value2", WORD_2 ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( index.query()
				.where( f -> f.named( "match-both-fields" )
						.param( "value1", WORD_1 )
						.param( "value2", WORD_5 ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );
	}

	@Test
	public void nested() {
		assertThatQuery( index.query()
				.where( f -> f.named( "nested.match-both-fields" )
						.param( "value1", WORD_1 )
						.param( "value2", WORD_2 ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( index.query()
				.where( f -> f.named( "nested.match-both-fields" )
						.param( "value1", WORD_1 )
						.param( "value2", WORD_5 ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );
		// This checks that we apply the nested predicate around the named predicate
		assertThatQuery( index.query()
				.where( f -> f.named( "nested.match-both-fields" )
						.param( "value1", WORD_1 )
						.param( "value2", WORD_4 ) ) )
				.hasNoHits();
	}

	@Test
	public void flattened() {
		assertThatQuery( index.query()
				.where( f -> f.named( "flattened.match-both-fields" )
						.param( "value1", WORD_1 )
						.param( "value2", WORD_2 ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		assertThatQuery( index.query()
				.where( f -> f.named( "flattened.match-both-fields" )
						.param( "value1", WORD_1 )
						.param( "value2", WORD_5 ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2 );
		assertThatQuery( index.query()
				.where( f -> f.named( "flattened.match-both-fields" )
						.param( "value1", WORD_1 )
						.param( "value2", WORD_4 ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	public void nullPath() {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> f.named( null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContainingAll( "must not be null" );
	}

	@Test
	public void unknownField() {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> f.named( "unknown_field.my-predicate" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unknown field", "'unknown_field'" );
	}

	@Test
	public void unknownPredicate_root() {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> f.named( "unknown-predicate" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Cannot use 'predicate:named:unknown-predicate' on index schema root" );
	}

	@Test
	public void unknownPredicate_objectField() {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> f.named( "nested.unknown-predicate" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Cannot use 'predicate:named:unknown-predicate' on field 'nested'" );
	}

	@Test
	public void unknownPredicate_valueField() {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThatThrownBy( () -> f.named( "field1.unknown-predicate" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Cannot use 'predicate:named:unknown-predicate' on field 'field1'" );
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> field1;
		final SimpleFieldModel<String> field2;
		final ObjectFieldBinding nested;
		final ObjectFieldBinding flattened;

		IndexBinding(IndexSchemaElement root) {
			field1 = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( root, "field1" );
			field2 = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( root, "field2" );
			root.namedPredicate( "match-both-fields",
					new TestPredicateDefinition( "field1", "field2" ) );

			nested = ObjectFieldBinding.create( root, "nested", ObjectStructure.NESTED );
			flattened = ObjectFieldBinding.create( root, "flattened", ObjectStructure.FLATTENED );
		}

		protected void initDocument(DocumentElement document, String value1, String value2,
				String value3, String value4) {
			document.addValue( field1.reference, value1 );
			document.addValue( field2.reference, value2 );

			DocumentElement nestedObject1 = document.addObject( nested.reference );
			nestedObject1.addValue( nested.field1.reference, value1 );
			nestedObject1.addValue( nested.field2.reference, value2 );
			DocumentElement nestedObject2 = document.addObject( nested.reference );
			nestedObject2.addValue( nested.field1.reference, value3 );
			nestedObject2.addValue( nested.field2.reference, value4 );

			DocumentElement flattenedObject1 = document.addObject( flattened.reference );
			flattenedObject1.addValue( flattened.field1.reference, value1 );
			flattenedObject1.addValue( flattened.field2.reference, value2 );
			DocumentElement flattenedObject2 = document.addObject( flattened.reference );
			flattenedObject2.addValue( flattened.field1.reference, value3 );
			flattenedObject2.addValue( flattened.field2.reference, value4 );
		}
	}

	static class ObjectFieldBinding {
		final SimpleFieldModel<String> field1;
		final SimpleFieldModel<String> field2;
		final IndexObjectFieldReference reference;

		static ObjectFieldBinding create(IndexSchemaElement parent, String relativeFieldName,
				ObjectStructure structure) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure ).multiValued();
			return new ObjectFieldBinding( objectField );
		}

		ObjectFieldBinding(IndexSchemaObjectField objectField) {
			reference = objectField.toReference();
			field1 = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( objectField, "field1" );
			field2 = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( objectField, "field2" );
			objectField.namedPredicate( "match-both-fields",
					new TestPredicateDefinition( "field1", "field2" ) );
		}
	}

	private static final class DataSet extends AbstractPredicateDataSet {
		public DataSet() {
			super( null );
		}

		public void contribute(BulkIndexer indexer) {
			indexer
					.add( DOCUMENT_1, document -> index.binding().initDocument( document, WORD_1, WORD_2, WORD_3, WORD_4 ) )
					.add( DOCUMENT_2, document -> index.binding().initDocument( document, WORD_1, WORD_5, WORD_3, WORD_5 ) )
					.add( EMPTY, document -> {} );
		}
	}

	public static class TestPredicateDefinition implements PredicateDefinition {

		private final String field1Name;
		private final String field2Name;

		public TestPredicateDefinition(String field1Name, String field2Name) {
			this.field1Name = field1Name;
			this.field2Name = field2Name;
		}

		@Override
		public SearchPredicate create(PredicateDefinitionContext context) {
			String word1 = context.param( "value1", String.class );
			String word2 = context.param( "value2", String.class );
			SearchPredicateFactory f = context.predicate();
			return f.and(
					f.match().field( field1Name ).matching( word1 ),
					f.match().field( field2Name ).matching( word2 )
			).toPredicate();
		}
	}

}
