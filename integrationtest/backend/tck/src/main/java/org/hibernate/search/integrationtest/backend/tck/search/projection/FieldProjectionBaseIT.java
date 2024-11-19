/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class FieldProjectionBaseIT {
	//CHECKSTYLE:ON

	private static final List<
			FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> supportedFieldTypes =
					FieldTypeDescriptor.getAll();

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static <F> FieldProjectionTestValues<F> testValues(FieldTypeDescriptor<F, ?> fieldType) {
		return new FieldProjectionTestValues<>( fieldType );
	}

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes( InObjectProjectionConfigured.mainIndex, InObjectProjectionConfigured.missingLevel1Index,
						InObjectProjectionConfigured.missingLevel1SingleValuedFieldIndex,
						InObjectProjectionConfigured.missingLevel2Index,
						InObjectProjectionConfigured.missingLevel2SingleValuedFieldIndex,
						InvalidFieldConfigured.index,
						ProjectableConfigured.projectableDefaultIndex, ProjectableConfigured.projectableYesIndex,
						ProjectableConfigured.projectableNoIndex,
						CardinalityCheckConfigured.index,
						CardinalityCheckConfigured.containing )
				.setup();

		BulkIndexer compositeForEachMainIndexer = InObjectProjectionConfigured.mainIndex.bulkIndexer();
		BulkIndexer compositeForEachMissingLevel1Indexer = InObjectProjectionConfigured.missingLevel1Index.bulkIndexer();
		BulkIndexer compositeForEachMissingLevel1SingleValuedFieldIndexer =
				InObjectProjectionConfigured.missingLevel1SingleValuedFieldIndex.bulkIndexer();
		BulkIndexer compositeForEachMissingLevel2Indexer = InObjectProjectionConfigured.missingLevel2Index.bulkIndexer();
		BulkIndexer compositeForEachMissingLevel2SingleValuedFieldIndexer =
				InObjectProjectionConfigured.missingLevel2SingleValuedFieldIndex.bulkIndexer();
		InObjectProjectionConfigured.dataSets
				.forEach( d -> d.contribute( InObjectProjectionConfigured.mainIndex, compositeForEachMainIndexer,
						InObjectProjectionConfigured.missingLevel1Index, compositeForEachMissingLevel1Indexer,
						InObjectProjectionConfigured.missingLevel1SingleValuedFieldIndex,
						compositeForEachMissingLevel1SingleValuedFieldIndexer,
						InObjectProjectionConfigured.missingLevel2Index, compositeForEachMissingLevel2Indexer,
						InObjectProjectionConfigured.missingLevel2SingleValuedFieldIndex,
						compositeForEachMissingLevel2SingleValuedFieldIndexer ) );

		compositeForEachMainIndexer.join( compositeForEachMissingLevel1Indexer,
				compositeForEachMissingLevel1SingleValuedFieldIndexer, compositeForEachMissingLevel2Indexer,
				compositeForEachMissingLevel2SingleValuedFieldIndexer,
				CardinalityCheckConfigured.containing.binding()
						.contribute( CardinalityCheckConfigured.containing.bulkIndexer() ),
				CardinalityCheckConfigured.index.binding()
						.contribute( CardinalityCheckConfigured.index.bulkIndexer() )
		);
	}

	@Nested
	class InObjectProjectionIT<F> extends InObjectProjectionConfigured<F> {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class InObjectProjectionConfigured<F>
			extends AbstractProjectionInObjectProjectionIT<F, F, FieldProjectionTestValues<F>> {
		private static final List<StandardFieldTypeDescriptor<?>> supportedFieldTypes = FieldTypeDescriptor.getAllStandard();
		private static final SimpleMappedIndex<IndexBinding> mainIndex =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "main" );
		private static final SimpleMappedIndex<MissingLevel1IndexBinding> missingLevel1Index =
				SimpleMappedIndex.of( MissingLevel1IndexBinding::new )
						.name( "missingLevel1" );
		private static final SimpleMappedIndex<MissingLevel1SingleValuedFieldIndexBinding> missingLevel1SingleValuedFieldIndex =
				SimpleMappedIndex.of( root -> new MissingLevel1SingleValuedFieldIndexBinding( root, supportedFieldTypes ) )
						.name( "missingLevel1Field1" );
		private static final SimpleMappedIndex<MissingLevel2IndexBinding> missingLevel2Index =
				SimpleMappedIndex.of( root -> new MissingLevel2IndexBinding( root, supportedFieldTypes ) )
						.name( "missingLevel2" );
		private static final SimpleMappedIndex<MissingLevel2SingleValuedFieldIndexBinding> missingLevel2SingleValuedFieldIndex =
				SimpleMappedIndex.of( root -> new MissingLevel2SingleValuedFieldIndexBinding( root, supportedFieldTypes ) )
						.name( "missingLevel2Field1" );

		private static final List<DataSet<?, ?, ?>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?, ?> fieldType : supportedFieldTypes ) {
				for ( ObjectStructure singleValuedObjectStructure : new ObjectStructure[] {
						ObjectStructure.FLATTENED,
						ObjectStructure.NESTED } ) {
					ObjectStructure multiValuedObjectStructure =
							ObjectStructure.NESTED.equals( singleValuedObjectStructure )
									|| TckConfiguration.get().getBackendFeatures()
											.reliesOnNestedDocumentsForMultiValuedObjectProjection()
													? ObjectStructure.NESTED
													: ObjectStructure.FLATTENED;
					DataSet<?, ?, ?> dataSet =
							new DataSet<>( testValues( fieldType ), singleValuedObjectStructure, multiValuedObjectStructure );
					dataSets.add( dataSet );
					parameters.add( Arguments.of( mainIndex, missingLevel1Index, missingLevel1SingleValuedFieldIndex,
							missingLevel2Index,
							missingLevel2SingleValuedFieldIndex,
							dataSet ) );
				}
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected RecursiveComparisonConfiguration.Builder configureRecursiveComparison(
				RecursiveComparisonConfiguration.Builder builder) {
			return builder.withComparatorForType( Comparator.nullsFirst( Comparator.naturalOrder() ), BigDecimal.class );
		}

		@Override
		protected ProjectionFinalStep<F> singleValuedProjection(SearchProjectionFactory<?, ?> f,
				String absoluteFieldPath, DataSet<F, F, FieldProjectionTestValues<F>> dataSet) {
			return f.field( absoluteFieldPath, dataSet.fieldType.getJavaType() );
		}

		@Override
		protected ProjectionFinalStep<List<F>> multiValuedProjection(SearchProjectionFactory<?, ?> f,
				String absoluteFieldPath, DataSet<F, F, FieldProjectionTestValues<F>> dataSet) {
			return f.field( absoluteFieldPath, dataSet.fieldType.getJavaType() ).multi();
		}

	}

	@Nested
	class InvalidFieldIT extends InvalidFieldConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class InvalidFieldConfigured extends AbstractProjectionInvalidFieldIT {
		private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
				.name( "invalidField" );

		public InvalidFieldConfigured() {
			super( index );
		}

		@Override
		protected void tryProjection(SearchProjectionFactory<?, ?> f, String fieldPath) {
			f.field( fieldPath );
		}

		@Override
		protected String projectionTrait() {
			return "projection:field";
		}
	}

	@Nested
	class ProjectableIT extends ProjectableConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class ProjectableConfigured extends AbstractProjectionProjectableIT {
		private static final SimpleMappedIndex<ProjectableDefaultIndexBinding> projectableDefaultIndex =
				SimpleMappedIndex.of( root -> new ProjectableDefaultIndexBinding( root, supportedFieldTypes ) )
						.name( "projectableDefault" );
		private static final SimpleMappedIndex<ProjectableYesIndexBinding> projectableYesIndex =
				SimpleMappedIndex.of( root -> new ProjectableYesIndexBinding( root, supportedFieldTypes ) )
						.name( "projectableYes" );

		private static final SimpleMappedIndex<ProjectableNoIndexBinding> projectableNoIndex =
				SimpleMappedIndex.of( root -> new ProjectableNoIndexBinding( root, supportedFieldTypes ) )
						.name( "projectableNo" );

		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?, ?> fieldType : supportedFieldTypes ) {
				parameters.add( Arguments.of( projectableDefaultIndex, projectableYesIndex, projectableNoIndex, fieldType ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@Override
		protected void tryProjection(SearchProjectionFactory<?, ?> f, String fieldPath, FieldTypeDescriptor<?, ?> fieldType) {
			f.field( fieldPath );
		}

		@Override
		protected String projectionTrait() {
			return "projection:field";
		}
	}

	@Nested
	class CardinalityCheckIT extends CardinalityCheckConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class CardinalityCheckConfigured {
		private static final SimpleMappedIndex<IndexContaining> containing =
				SimpleMappedIndex.of( IndexContaining::new ).name( "cardinality-containing" );
		private static final SimpleMappedIndex<Index> index = SimpleMappedIndex.of( Index::new ).name( "cardinality-index" );

		/**
		 * In this case we build a projection with object/fields projections recreating the entity tree structure.
		 * Hence, it should be close to what a projection constructor is doing.
		 */
		@Test
		void testAsIfItWasProjectionConstructors() {
			List<List<?>> hits = index.createScope().query()
					.select( f -> f.composite(
							f.id(),
							f.field( "string" ),
							f.object( "contained" ).from(
									f.field( "contained.id" ),
									f.field( "contained.containedString" ),
									f.object( "contained.deeperContained" ).from(
											f.field( "contained.deeperContained.id" ),
											f.field( "contained.deeperContained.deeperContainedString" )
									).asList()
							).asList().multi()
					) )
					.where( f -> f.matchAll() )
					.fetchAllHits();

			assertThat( hits ).hasSize( 1 )
					.contains(
							List.of(
									"1",
									"string",
									List.of(
											List.of( "10", "containedString1",
													List.of( "100", "deeperContainedString1" ) ),
											List.of( "20", "containedString2",
													List.of( "200", "deeperContainedString2" ) )
									)
							)
					);
		}

		/**
		 * We want to make sure that cardinality is correctly checked when we request a projection for a single field with a "long" path
		 * containing both single and multi fields in it.
		 * <p>
		 * Since there is a multi-`contained` the resulting caridnality of the projected field is expected to be also multi
		 */
		@Test
		void testFieldProjectionLongPath_correctCardinality() {
			List<List<Object>> hits = index.createScope().query()
					.select( f -> f.field( "contained.deeperContained.id" ).multi() )
					.where( f -> f.matchAll() )
					.fetchAllHits();

			assertThat( hits ).hasSize( 1 )
					.contains( List.of( "100", "200" ) );
		}

		@Test
		void testFieldProjectionLongPath_incorrectCardinality() {
			assertThatThrownBy( () -> index.createScope().query()
					.select( f -> f.field( "contained.deeperContained.id" ) )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "Invalid cardinality for projection on field 'contained.deeperContained.id'" );
		}

		@Test
		void testFieldProjectionLongPath_correctCardinality_multiAtDifferentLevelInPath() {
			assertThat( containing.createScope().query()
					.select( f -> f.field( "single.contained.deeperContained.id" ).multi() )
					.where( f -> f.matchAll() )
					.fetchAllHits()
			).hasSize( 1 )
					.contains( List.of( "100", "200" ) );
		}

		@Test
		void testFieldProjectionLongPath_correctCardinality_multiAtDifferentLevelInPath_multipleMultis() {
			assertThat( containing.createScope().query()
					.select( f -> f.field( "list.contained.deeperContained.id" ).multi() )
					.where( f -> f.matchAll() )
					.fetchAllHits() ).hasSize( 1 )
					.contains( List.of( "100", "200" ) );
		}

		/**
		 * Here we take all multi fields as object projections with expected cardinalities and then
		 * add the "deepest" field as a simple single-valued field:
		 */
		@Test
		void testFieldProjectionLongPath_correctCardinality_multiFieldsAsObjects() {
			assertThat( containing.createScope().query()
					.select( f -> f.object( "list" )
							.from( f.object( "list.contained" )
									.from( f.field( "list.contained.deeperContained.id" ) ).asList().multi() )
							.asList().multi() )
					.where( f -> f.matchAll() )
					.fetchAllHits() ).hasSize( 1 )
					.contains(
							List.of( List.of( List.of( List.of( "100" ), List.of( "200" ) ) ) ) );
		}

		@Test
		void testFieldProjectionLongPath_incorrectCardinality_multiAtDifferentLevelInPath_multipleMultis() {
			assertThatThrownBy( () -> containing.createScope().query()
					.select( f -> f.field( "list.contained.deeperContained.id" ) )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining(
							"Invalid cardinality for projection on field 'list.contained.deeperContained.id'" );
		}

		@Test
		void testFieldProjectionLongPath_incorrectCardinality_multiAtDifferentLevelInPath() {
			assertThatThrownBy( () -> containing.createScope().query()
					.select( f -> f.field( "single.contained.deeperContained.id" ) )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining(
							"Invalid cardinality for projection on field 'single.contained.deeperContained.id'" );
		}

		public static final class IndexContaining extends IndexBase {
			public IndexContaining(IndexSchemaElement root) {
				IndexSchemaObjectField single = root.objectField( "single", ObjectStructure.NESTED );
				addIndexFields( single );
				single.toReference();

				IndexSchemaObjectField list = root.objectField( "list", ObjectStructure.NESTED ).multiValued();
				addIndexFields( list );
				list.toReference();
			}

			BulkIndexer contribute(BulkIndexer indexer) {
				indexer.add( "1", d -> {
					contribute( d.addObject( "single" ) );
					contribute( d.addObject( "list" ) );
				} );
				return indexer;
			}
		}

		public static class Index extends IndexBase {

			public Index(IndexSchemaElement root) {
				addIndexFields( root );
			}

			BulkIndexer contribute(BulkIndexer indexer) {
				indexer.add( "1", this::contribute );
				return indexer;
			}
		}

		public abstract static class IndexBase {
			protected void addIndexFields(IndexSchemaElement el) {
				el.field( "id", f -> f.asString().projectable( Projectable.YES ) ).toReference();
				el.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();

				IndexSchemaObjectField contained = el.objectField( "contained", ObjectStructure.NESTED ).multiValued();
				contained.field( "id", f -> f.asString().projectable( Projectable.YES ) ).toReference();
				contained.field( "containedString", f -> f.asString().projectable( Projectable.YES ) ).toReference();

				IndexSchemaObjectField deeperContained = contained.objectField( "deeperContained", ObjectStructure.NESTED );
				deeperContained.field( "id", f -> f.asString().projectable( Projectable.YES ) ).toReference();
				deeperContained.field( "deeperContainedString", f -> f.asString().projectable( Projectable.YES ) )
						.toReference();

				deeperContained.toReference();
				contained.toReference();
			}

			protected void contribute(DocumentElement element) {
				element.addValue( "id", "1" );
				element.addValue( "string", "string" );

				DocumentElement contained = element.addObject( "contained" );
				contained.addValue( "id", "10" );
				contained.addValue( "containedString", "containedString1" );
				DocumentElement deeperContained = contained.addObject( "deeperContained" );
				deeperContained.addValue( "id", "100" );
				deeperContained.addValue( "deeperContainedString", "deeperContainedString1" );

				contained = element.addObject( "contained" );
				contained.addValue( "id", "20" );
				contained.addValue( "containedString", "containedString2" );
				deeperContained = contained.addObject( "deeperContained" );
				deeperContained.addValue( "id", "200" );
				deeperContained.addValue( "deeperContainedString", "deeperContainedString2" );

			}
		}
	}
}
