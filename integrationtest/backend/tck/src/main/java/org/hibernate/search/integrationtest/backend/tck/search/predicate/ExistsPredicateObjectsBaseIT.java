/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.function.TriFunction;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.runner.nested.Nested;
import org.hibernate.search.util.impl.test.runner.nested.NestedRunner;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(NestedRunner.class)
public class ExistsPredicateObjectsBaseIT {

	private static final FieldTypeDescriptor<String> innerFieldType = AnalyzedStringFieldTypeDescriptor.INSTANCE;

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	@BeforeClass
	public static void setup() {
		setupHelper.start()
				.withIndexes(
						InObjectFieldIT.mainIndex, InObjectFieldIT.missingFieldIndex,
						ScoreIT.index
				)
				.setup();

		final BulkIndexer inObjectFieldMainIndexer = InObjectFieldIT.mainIndex.bulkIndexer();
		final BulkIndexer inObjectFieldMissingFieldIndexer = InObjectFieldIT.missingFieldIndex.bulkIndexer();
		InObjectFieldIT.dataSets.forEach( d -> d.contribute( inObjectFieldMainIndexer, inObjectFieldMissingFieldIndexer ) );

		final BulkIndexer scoreIndexer = ScoreIT.index.bulkIndexer();
		ScoreIT.dataSets.forEach( d -> d.contribute( scoreIndexer ) );

		inObjectFieldMainIndexer.join( inObjectFieldMissingFieldIndexer, scoreIndexer );
	}

	@Test
	public void takariCpSuiteWorkaround() {
		// Workaround to get Takari-CPSuite to run this test.
	}

	@Nested
	@RunWith(Parameterized.class)
	public static class InObjectFieldIT extends AbstractPredicateInObjectFieldIT {
		private static final List<DataSet> dataSets = new ArrayList<>();
		private static final List<Object[]> parameters = new ArrayList<>();
		static {
			for ( ObjectStructure structure : Arrays.asList( ObjectStructure.NESTED, ObjectStructure.FLATTENED ) ) {
				DataSet dataSet = new DataSet( structure );
				dataSets.add( dataSet );
				parameters.add( new Object[] { dataSet } );
			}
		}

		private static final SimpleMappedIndex<IndexBinding> mainIndex =
				SimpleMappedIndex.of( root -> new IndexBinding( root, Collections.singletonList( innerFieldType ) ) )
						.name( "nesting" );

		private static final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex =
				SimpleMappedIndex
						.of( root -> new MissingFieldIndexBinding( root, Collections.singletonList( innerFieldType ) ) )
						.name( "nesting_missingField" );

		@Parameterized.Parameters(name = "{0}")
		public static List<Object[]> parameters() {
			return parameters;
		}

		private final DataSet dataSet;

		public InObjectFieldIT(DataSet dataSet) {
			super( mainIndex, missingFieldIndex, dataSet );
			this.dataSet = dataSet;
		}

		@Test
		@TestForIssue(jiraKey = "HSEARCH-4162")
		public void factoryWithRoot_nested() {
			assertThatQuery( mainIndex.query()
					.where( f -> predicateWithRelativePath( f.withRoot( binding.nested.absolutePath ), binding.nested ) )
					.routing( dataSet.routingKey ) )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
		}

		@Test
		@TestForIssue(jiraKey = "HSEARCH-4162")
		public void factoryWithRoot_flattened() {
			assertThatQuery( mainIndex.query()
					.where( f -> predicateWithRelativePath( f.withRoot( binding.flattened.absolutePath ), binding.flattened ) )
					.routing( dataSet.routingKey ) )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, ObjectFieldBinding objectFieldBinding,
				int matchingDocOrdinal) {
			if ( matchingDocOrdinal != 0 ) {
				throw new IllegalStateException( "This predicate can only match the first document" );
			}
			return f.exists().field( targetField( objectFieldBinding ).absolutePath );
		}

		protected PredicateFinalStep predicateWithRelativePath(SearchPredicateFactory f,
				ObjectFieldBinding objectFieldBinding) {
			return f.exists().field( targetField( objectFieldBinding ).relativeName );
		}

		private ObjectFieldBinding targetField(ObjectFieldBinding objectFieldBinding) {
			switch ( dataSet.structure ) {
				case FLATTENED:
					return objectFieldBinding.flattened;
				case NESTED:
					return objectFieldBinding.nested;
				default:
					throw new IllegalStateException( "Unexpected structure: " + dataSet.structure );
			}
		}

		private static class DataSet extends AbstractPredicateDataSet {
			final ObjectStructure structure;

			protected DataSet(ObjectStructure structure) {
				super( structure.name() );
				this.structure = structure;
			}

			public void contribute(BulkIndexer mainIndexer, BulkIndexer missingFieldIndexer) {
				mainIndexer.add( docId( 0 ), routingKey, document -> mainIndex.binding()
						.initDocument( document, innerFieldType, "irrelevant" ) );
				mainIndexer.add( docId( 1 ), routingKey, document -> {} );
				missingFieldIndexer.add( docId( MISSING_FIELD_INDEX_DOC_ORDINAL ), routingKey,
						document -> missingFieldIndex.binding().initDocument() );
			}
		}
	}

	@Nested
	@RunWith(Parameterized.class)
	public static class ScoreIT extends AbstractPredicateScoreIT {
		private static final List<DataSet> dataSets = new ArrayList<>();
		private static final List<Object[]> parameters = new ArrayList<>();
		static {
			for ( ObjectStructure structure : ObjectStructure.values() ) {
				DataSet dataSet = new DataSet( structure );
				dataSets.add( dataSet );
				parameters.add( new Object[] { dataSet } );
			}
		}

		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "score" );

		@Parameterized.Parameters(name = "{0}")
		public static List<Object[]> parameters() {
			return parameters;
		}

		protected final DataSet dataSet;

		public ScoreIT(DataSet dataSet) {
			super( index, dataSet );
			this.dataSet = dataSet;
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, int matchingDocOrdinal) {
			return f.exists().field( fieldPath( matchingDocOrdinal ) );
		}

		@Override
		protected PredicateFinalStep predicateWithBoost(SearchPredicateFactory f, int matchingDocOrdinal,
				float boost) {
			return f.exists().field( fieldPath( matchingDocOrdinal ) ).boost( boost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, int matchingDocOrdinal) {
			return f.exists().field( fieldPath( matchingDocOrdinal ) ).constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndBoost(SearchPredicateFactory f,
				int matchingDocOrdinal, float boost) {
			return f.exists().field( fieldPath( matchingDocOrdinal ) ).constantScore().boost( boost );
		}

		private String fieldPath(int matchingDocOrdinal) {
			Map<ObjectStructure, ObjectFieldBinding> field;
			switch ( matchingDocOrdinal ) {
				case 0:
					field = index.binding().field0;
					break;
				case 1:
					field = index.binding().field1;
					break;
				default:
					throw new IllegalStateException( "This test only works with up to two documents" );
			}
			return field.get( dataSet.structure ).relativeFieldName;
		}

		private static class IndexBinding {
			final Map<ObjectStructure, ObjectFieldBinding> field0;
			final Map<ObjectStructure, ObjectFieldBinding> field1;

			IndexBinding(IndexSchemaElement root) {
				field0 = createByStructure( ObjectFieldBinding::create, root, "field0_" );
				field1 = createByStructure( ObjectFieldBinding::create, root, "field1_" );
			}
		}

		private static class ObjectFieldBinding {
			final IndexObjectFieldReference reference;
			final String relativeFieldName;
			final SimpleFieldModel<String> field;

			static ObjectFieldBinding create(IndexSchemaElement parent, String relativeFieldName, ObjectStructure structure) {
				IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
				return new ObjectFieldBinding( objectField, relativeFieldName );
			}

			ObjectFieldBinding(IndexSchemaObjectField self, String relativeFieldName) {
				reference = self.toReference();
				this.relativeFieldName = relativeFieldName;
				field = SimpleFieldModel.mapper( innerFieldType ).map( self, "field" );
			}
		}

		private static class DataSet extends AbstractPredicateDataSet {
			private final ObjectStructure structure;

			protected DataSet(ObjectStructure structure) {
				super( structure.name() );
				this.structure = structure;
			}

			public void contribute(BulkIndexer scoreIndexer) {
				IndexBinding binding = index.binding();
				ObjectFieldBinding field0Binding = binding.field0.get( structure );
				ObjectFieldBinding field1Binding = binding.field1.get( structure );
				scoreIndexer.add( docId( 0 ), routingKey, document -> {
					DocumentElement field0 = document.addObject( field0Binding.reference );
					field0.addValue( field0Binding.field.reference, "foo" );
					document.addObject( field1Binding.reference );
				} );
				scoreIndexer.add( docId( 1 ), routingKey, document -> {
					document.addObject( field0Binding.reference );
					DocumentElement field1 = document.addObject( field1Binding.reference );
					field1.addValue( field1Binding.field.reference, "foo" );
				} );
				scoreIndexer.add( docId( 2 ), routingKey, document -> {
					document.addObject( field0Binding.reference );
					document.addObject( field1Binding.reference );
				} );
			}
		}
	}

	static <T> Map<ObjectStructure, T> createByStructure(TriFunction<IndexSchemaElement, String, ObjectStructure, T> factory,
			IndexSchemaElement parent, String relativeNamePrefix) {
		Map<ObjectStructure, T> map = new LinkedHashMap<>();
		for ( ObjectStructure structure : ObjectStructure.values() ) {
			map.put( structure, factory.apply( parent, relativeNamePrefix + structure.name(), structure ) );
		}
		return map;
	}

}
