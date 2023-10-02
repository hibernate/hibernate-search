/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.function.TriFunction;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class ExistsPredicateObjectsBaseIT {
	//CHECKSTYLE:ON

	private static final StandardFieldTypeDescriptor<String> innerFieldType = AnalyzedStringFieldTypeDescriptor.INSTANCE;

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes(
						InObjectFieldConfigured.mainIndex, InObjectFieldConfigured.missingFieldIndex,
						ScoreConfigured.index
				)
				.setup();

		final BulkIndexer inObjectFieldMainIndexer = InObjectFieldConfigured.mainIndex.bulkIndexer();
		final BulkIndexer inObjectFieldMissingFieldIndexer = InObjectFieldConfigured.missingFieldIndex.bulkIndexer();
		InObjectFieldConfigured.dataSets
				.forEach( d -> d.contribute( inObjectFieldMainIndexer, inObjectFieldMissingFieldIndexer ) );

		final BulkIndexer scoreIndexer = ScoreConfigured.index.bulkIndexer();
		ScoreConfigured.dataSets.forEach( d -> d.contribute( scoreIndexer ) );

		inObjectFieldMainIndexer.join( inObjectFieldMissingFieldIndexer, scoreIndexer );
	}

	@Nested
	class InObjectFieldIT extends InObjectFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class InObjectFieldConfigured extends AbstractPredicateInObjectFieldIT {

		private static final SimpleMappedIndex<IndexBinding> mainIndex =
				SimpleMappedIndex.of( root -> new IndexBinding( root, Collections.singletonList( innerFieldType ) ) )
						.name( "nesting" );

		private static final SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex =
				SimpleMappedIndex
						.of( root -> new MissingFieldIndexBinding( root, Collections.singletonList( innerFieldType ) ) )
						.name( "nesting_missingField" );
		private static final List<DataSet> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( ObjectStructure structure : Arrays.asList( ObjectStructure.NESTED, ObjectStructure.FLATTENED ) ) {
				DataSet dataSet = new DataSet( structure );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( mainIndex, missingFieldIndex, dataSet ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@ParameterizedTest(name = "{2}")
		@MethodSource("params")
		@TestForIssue(jiraKey = "HSEARCH-4162")
		void factoryWithRoot_nested(SimpleMappedIndex<IndexBinding> mainIndex,
				SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
				AbstractPredicateDataSet dataSet) {
			assertThatQuery( mainIndex.query()
					.where( f -> predicateWithRelativePath( f.withRoot( mainIndex.binding().nested.absolutePath ),
							mainIndex.binding().nested,
							dataSet
					) )
					.routing( dataSet.routingKey ) )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
		}

		@ParameterizedTest(name = "{2}")
		@MethodSource("params")
		@TestForIssue(jiraKey = "HSEARCH-4162")
		void factoryWithRoot_flattened(SimpleMappedIndex<IndexBinding> mainIndex,
				SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
				AbstractPredicateDataSet dataSet) {
			assertThatQuery( mainIndex.query()
					.where( f -> predicateWithRelativePath( f.withRoot( mainIndex.binding().flattened.absolutePath ),
							mainIndex.binding().flattened,
							dataSet
					) )
					.routing( dataSet.routingKey ) )
					.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, ObjectFieldBinding objectFieldBinding,
				int matchingDocOrdinal, AbstractPredicateDataSet dataSet) {
			if ( matchingDocOrdinal != 0 ) {
				throw new IllegalStateException( "This predicate can only match the first document" );
			}
			return f.exists().field( targetField( objectFieldBinding, ( (DataSet) dataSet ) ).absolutePath );
		}

		protected PredicateFinalStep predicateWithRelativePath(SearchPredicateFactory f, ObjectFieldBinding objectFieldBinding,
				AbstractPredicateDataSet dataSet) {
			return f.exists().field( targetField( objectFieldBinding, ( (DataSet) dataSet ) ).relativeName );
		}

		private ObjectFieldBinding targetField(ObjectFieldBinding objectFieldBinding, DataSet dataSet) {
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
	class ScoreIT extends ScoreConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class ScoreConfigured extends AbstractPredicateScoreIT {
		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "score" );
		private static final List<DataSet> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( ObjectStructure structure : ObjectStructure.values() ) {
				DataSet dataSet = new DataSet( structure );
				dataSets.add( dataSet );
				parameters.add( Arguments.of( index, dataSet ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected PredicateFinalStep predicate(SearchPredicateFactory f, int matchingDocOrdinal,
				AbstractPredicateDataSet dataSet, StubMappedIndex index) {
			return f.exists().field( fieldPath( matchingDocOrdinal, dataSet ) );
		}

		@Override
		protected PredicateFinalStep predicateWithBoost(SearchPredicateFactory f, int matchingDocOrdinal,
				float boost, AbstractPredicateDataSet dataSet,
				StubMappedIndex index) {
			return f.exists().field( fieldPath( matchingDocOrdinal, dataSet ) ).boost( boost );
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScore(SearchPredicateFactory f, int matchingDocOrdinal,
				AbstractPredicateDataSet dataSet, StubMappedIndex index) {
			return f.exists().field( fieldPath( matchingDocOrdinal, dataSet ) ).constantScore();
		}

		@Override
		protected PredicateFinalStep predicateWithConstantScoreAndBoost(SearchPredicateFactory f,
				int matchingDocOrdinal, float boost, AbstractPredicateDataSet dataSet,
				StubMappedIndex index) {
			return f.exists().field( fieldPath( matchingDocOrdinal, dataSet ) ).constantScore().boost( boost );
		}

		private String fieldPath(int matchingDocOrdinal, AbstractPredicateDataSet dataSet) {
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
			return field.get( ( (DataSet) dataSet ).structure ).relativeFieldName;
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
