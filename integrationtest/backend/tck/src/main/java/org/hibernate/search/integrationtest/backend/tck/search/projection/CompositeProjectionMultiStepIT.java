/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponent1Step;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponent2Step;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponent3Step;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponent4Step;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponentsAtLeast1AddedStep;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.data.Pair;
import org.hibernate.search.util.impl.test.data.Triplet;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests composite projections created through the multi-step DSL,
 * e.g. {@code f.composite().add( otherProjection1 ).add( otherProjection2 ).transform( MyPair::new ) },
 * as opposed to the single-step DSL,
 * e.g. {@code f.composite( MyPair::new, otherProjection1, otherProjection2 ) },
 * which is tested in {@link CompositeProjectionSingleStepIT}.
 */
@RunWith(Enclosed.class)
public class CompositeProjectionMultiStepIT {

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		index.bulkIndexer()
				.add( DOCUMENT_1, document -> {
					index.binding().field1.document1Value.write( document );
					index.binding().field2.document1Value.write( document );
					index.binding().field3.document1Value.write( document );
					index.binding().field4.document1Value.write( document );
				} )
				.add( DOCUMENT_2, document -> {
					index.binding().field1.document2Value.write( document );
					index.binding().field2.document2Value.write( document );
					index.binding().field3.document2Value.write( document );
					index.binding().field4.document2Value.write( document );
				} )
				.add( DOCUMENT_3, document -> {
					index.binding().field1.document3Value.write( document );
					index.binding().field2.document3Value.write( document );
					index.binding().field3.document3Value.write( document );
					index.binding().field4.document3Value.write( document );
				} )
				.join();
	}

	@Test
	public void takariCpSuiteWorkaround() {
		// Workaround to get Takari-CPSuite to run this test.
	}

	interface AddVariant<S extends CompositeProjectionComponentsAtLeast1AddedStep> {
		S add(SearchProjectionFactory<?, ?> f, CompositeProjectionComponent1Step step);
	}

	private abstract static class AbstractAtLeast1AddedIT<S extends CompositeProjectionComponentsAtLeast1AddedStep> {

		@Parameterized.Parameter
		public AddVariant<S> addVariant;

		@Test
		public void asList() {
			assertThatQuery( index.query()
					.select( f -> addVariant.add( f, f.composite() ).asList() )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedLists() );
		}

		@Test
		public void transformList() {
			assertThatQuery( index.query()
					.select( f -> addVariant.add( f, f.composite() ).transformList( ValueWrapper<List<?>>::new ) )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedLists().stream().<ValueWrapper<List<?>>>map( ValueWrapper::new ).collect( Collectors.toList() ) );
		}

		protected abstract Collection<List<?>> expectedLists();

	}

	private abstract static class AbstractKnownNumberAddedIT<S extends CompositeProjectionComponentsAtLeast1AddedStep, T>
			extends AbstractAtLeast1AddedIT<S> {

		@Test
		public void transform() {
			assertThatQuery( index.query()
					.select( f -> transform( addVariant.add( f, f.composite() ) ) )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedTransformed() );
		}

		protected abstract ProjectionFinalStep<T> transform(S step);

		protected abstract Collection<T> expectedTransformed();

	}

	static Collection<List<?>> oneComponentExpectedLists() {
		return Arrays.asList(
				Collections.singletonList( index.binding().field1.document1Value.indexedValue ),
				Collections.singletonList( index.binding().field1.document2Value.indexedValue ),
				Collections.singletonList( index.binding().field1.document3Value.indexedValue )
		);
	}

	static Collection<ValueWrapper<String>> oneComponentExpectedTransformed() {
		return Arrays.asList(
				new ValueWrapper<>( index.binding().field1.document1Value.indexedValue ),
				new ValueWrapper<>( index.binding().field1.document2Value.indexedValue ),
				new ValueWrapper<>( index.binding().field1.document3Value.indexedValue )
		);
	}

	@RunWith(Parameterized.class)
	public static class OneComponentAddIndividuallyIT
			extends AbstractKnownNumberAddedIT<CompositeProjectionComponent2Step<String>, ValueWrapper<String>> {
		@Parameterized.Parameters
		public static List<AddVariant<CompositeProjectionComponent2Step<String>>> adds() {
			return Arrays.asList(
					(f, step) -> step.add( f.field( index.binding().field1.relativeFieldName, String.class ) )
			);
		}

		@Override
		protected Collection<List<?>> expectedLists() {
			return oneComponentExpectedLists();
		}

		@Override
		protected ProjectionFinalStep<ValueWrapper<String>> transform(CompositeProjectionComponent2Step<String> step) {
			return step.transform( ValueWrapper::new );
		}

		@Override
		protected Collection<ValueWrapper<String>> expectedTransformed() {
			return oneComponentExpectedTransformed();
		}
	}

	@RunWith(Parameterized.class)
	public static class OneComponentAddArrayIT
			extends AbstractAtLeast1AddedIT<CompositeProjectionComponentsAtLeast1AddedStep> {
		@Parameterized.Parameters
		public static List<AddVariant<?>> adds() {
			return Arrays.asList(
					(f, step) -> step.add( new ProjectionFinalStep<?>[] { f.field( index.binding().field1.relativeFieldName, String.class ) } ),
					(f, step) -> step.add( new SearchProjection<?>[] { f.field( index.binding().field1.relativeFieldName, String.class ).toProjection() } )
			);
		}

		@Override
		protected Collection<List<?>> expectedLists() {
			return oneComponentExpectedLists();
		}
	}

	static Collection<List<?>> twoComponentExpectedLists() {
		return Arrays.asList(
				Arrays.asList( index.binding().field1.document1Value.indexedValue,
						index.binding().field2.document1Value.indexedValue ),
				Arrays.asList( index.binding().field1.document2Value.indexedValue,
						index.binding().field2.document2Value.indexedValue ),
				Arrays.asList( index.binding().field1.document3Value.indexedValue,
						index.binding().field2.document3Value.indexedValue )
		);
	}

	static Collection<Pair<String, String>> twoComponentExpectedTransformed() {
		return Arrays.asList(
				new Pair<>( index.binding().field1.document1Value.indexedValue,
						index.binding().field2.document1Value.indexedValue ),
				new Pair<>( index.binding().field1.document2Value.indexedValue,
						index.binding().field2.document2Value.indexedValue ),
				new Pair<>( index.binding().field1.document3Value.indexedValue,
						index.binding().field2.document3Value.indexedValue )
		);
	}

	@RunWith(Parameterized.class)
	public static class TwoComponentsAddIndividuallyIT
			extends AbstractKnownNumberAddedIT<CompositeProjectionComponent3Step<String, String>, Pair<String, String>> {
		@Parameterized.Parameters
		public static List<AddVariant<CompositeProjectionComponent3Step<String, String>>> adds() {
			return Arrays.asList(
					(f, step) -> step.add( f.field( index.binding().field1.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field2.relativeFieldName, String.class ) ),
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ),
									f.field( index.binding().field2.relativeFieldName, String.class ) )
			);
		}

		@Override
		protected Collection<List<?>> expectedLists() {
			return twoComponentExpectedLists();
		}

		@Override
		protected ProjectionFinalStep<Pair<String, String>> transform(CompositeProjectionComponent3Step<String, String> step) {
			return step.transform( Pair::new );
		}

		@Override
		protected Collection<Pair<String, String>> expectedTransformed() {
			return twoComponentExpectedTransformed();
		}
	}

	@RunWith(Parameterized.class)
	public static class TwoComponentsAddArrayIT
			extends AbstractAtLeast1AddedIT<CompositeProjectionComponentsAtLeast1AddedStep> {
		@Parameterized.Parameters
		public static List<AddVariant<?>> adds() {
			return Arrays.asList(
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ),
									f.field( index.binding().field2.relativeFieldName, String.class ) ),
					(f, step) -> step
							.add( new ProjectionFinalStep<?>[] { f.field( index.binding().field1.relativeFieldName, String.class ) } )
							.add( new ProjectionFinalStep<?>[] { f.field( index.binding().field2.relativeFieldName, String.class ) } ),
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ) )
							.add( new ProjectionFinalStep<?>[] { f.field( index.binding().field2.relativeFieldName, String.class ) } ),
					(f, step) -> step
							.add( new ProjectionFinalStep<?>[] { f.field( index.binding().field1.relativeFieldName, String.class ) } )
							.add( f.field( index.binding().field2.relativeFieldName, String.class ) )
			);
		}

		@Override
		protected Collection<List<?>> expectedLists() {
			return twoComponentExpectedLists();
		}
	}

	static Collection<List<?>> threeComponentExpectedLists() {
		return Arrays.asList(
				Arrays.asList( index.binding().field1.document1Value.indexedValue,
						index.binding().field2.document1Value.indexedValue,
						index.binding().field3.document1Value.indexedValue ),
				Arrays.asList( index.binding().field1.document2Value.indexedValue,
						index.binding().field2.document2Value.indexedValue,
						index.binding().field3.document2Value.indexedValue ),
				Arrays.asList( index.binding().field1.document3Value.indexedValue,
						index.binding().field2.document3Value.indexedValue,
						index.binding().field3.document3Value.indexedValue )
		);
	}

	static Collection<Triplet<String, String, LocalDate>> threeComponentExpectedTransformed() {
		return Arrays.asList(
				new Triplet<>( index.binding().field1.document1Value.indexedValue,
						index.binding().field2.document1Value.indexedValue,
						index.binding().field3.document1Value.indexedValue ),
				new Triplet<>( index.binding().field1.document2Value.indexedValue,
						index.binding().field2.document2Value.indexedValue,
						index.binding().field3.document2Value.indexedValue ),
				new Triplet<>( index.binding().field1.document3Value.indexedValue,
						index.binding().field2.document3Value.indexedValue,
						index.binding().field3.document3Value.indexedValue )
		);
	}

	@RunWith(Parameterized.class)
	public static class ThreeComponentsAddIndividuallyIT
			extends AbstractKnownNumberAddedIT<
					CompositeProjectionComponent4Step<String, String, LocalDate>,
					Triplet<String, String, LocalDate>
			> {
		@Parameterized.Parameters
		public static List<AddVariant<CompositeProjectionComponent4Step<String, String, LocalDate>>> adds() {
			return Arrays.asList(
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field2.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field3.relativeFieldName, LocalDate.class ) ),
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ),
									f.field( index.binding().field2.relativeFieldName, String.class ),
									f.field( index.binding().field3.relativeFieldName, LocalDate.class ) ),
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field2.relativeFieldName, String.class ),
									f.field( index.binding().field3.relativeFieldName, LocalDate.class ) ),
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ),
									f.field( index.binding().field2.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field3.relativeFieldName, LocalDate.class ) )
			);
		}

		@Override
		protected Collection<List<?>> expectedLists() {
			return threeComponentExpectedLists();
		}

		@Override
		protected ProjectionFinalStep<Triplet<String, String, LocalDate>> transform(
				CompositeProjectionComponent4Step<String, String, LocalDate> step) {
			return step.transform( Triplet::new );
		}

		@Override
		protected Collection<Triplet<String, String, LocalDate>> expectedTransformed() {
			return threeComponentExpectedTransformed();
		}
	}

	@RunWith(Parameterized.class)
	public static class ThreeComponentsAddArrayIT
			extends AbstractAtLeast1AddedIT<CompositeProjectionComponentsAtLeast1AddedStep> {
		@Parameterized.Parameters
		public static List<AddVariant<?>> adds() {
			return Arrays.asList(
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ),
									f.field( index.binding().field2.relativeFieldName, String.class ),
									f.field( index.binding().field3.relativeFieldName, LocalDate.class ) ),
					(f, step) -> step
							.add( new ProjectionFinalStep<?>[] { f.field( index.binding().field1.relativeFieldName, String.class ) } )
							.add( new ProjectionFinalStep<?>[] { f.field( index.binding().field2.relativeFieldName, String.class ) } )
							.add( new ProjectionFinalStep<?>[] { f.field( index.binding().field3.relativeFieldName, LocalDate.class ) } ),
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field2.relativeFieldName, String.class ),
									f.field( index.binding().field3.relativeFieldName, LocalDate.class ) ),
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field2.relativeFieldName, String.class ) )
							.add( new ProjectionFinalStep<?>[] { f.field( index.binding().field3.relativeFieldName, LocalDate.class ) } ),
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ),
									f.field( index.binding().field2.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field3.relativeFieldName, LocalDate.class ) )
			);
		}

		@Override
		protected Collection<List<?>> expectedLists() {
			return threeComponentExpectedLists();
		}
	}

	static Collection<List<?>> fourComponentExpectedLists() {
		return Arrays.asList(
				Arrays.asList( index.binding().field1.document1Value.indexedValue,
						index.binding().field2.document1Value.indexedValue,
						index.binding().field3.document1Value.indexedValue,
						index.binding().field4.document1Value.indexedValue ),
				Arrays.asList( index.binding().field1.document2Value.indexedValue,
						index.binding().field2.document2Value.indexedValue,
						index.binding().field3.document2Value.indexedValue,
						index.binding().field4.document2Value.indexedValue ),
				Arrays.asList( index.binding().field1.document3Value.indexedValue,
						index.binding().field2.document3Value.indexedValue,
						index.binding().field3.document3Value.indexedValue,
						index.binding().field4.document3Value.indexedValue )
		);
	}

	@RunWith(Parameterized.class)
	public static class FourComponentsAddIndividuallyIT
			extends AbstractAtLeast1AddedIT<CompositeProjectionComponentsAtLeast1AddedStep> {
		@Parameterized.Parameters
		public static List<AddVariant<?>> adds() {
			return Arrays.asList(
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field2.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field3.relativeFieldName, LocalDate.class ) )
							.add( f.field( index.binding().field4.relativeFieldName, String.class ) ),
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field2.relativeFieldName, String.class ),
									f.field( index.binding().field3.relativeFieldName, LocalDate.class ),
									f.field( index.binding().field4.relativeFieldName, String.class ) ),
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field2.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field3.relativeFieldName, LocalDate.class ),
									f.field( index.binding().field4.relativeFieldName, String.class ) ),
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field2.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field3.relativeFieldName, LocalDate.class ) )
							.add( new ProjectionFinalStep<?>[] { f.field( index.binding().field4.relativeFieldName, String.class ) } ),
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ) )
							.add( f.field( index.binding().field2.relativeFieldName, String.class ),
									f.field( index.binding().field3.relativeFieldName, LocalDate.class ) )
							.add( f.field( index.binding().field4.relativeFieldName, String.class ) ),
					(f, step) -> step
							.add( f.field( index.binding().field1.relativeFieldName, String.class ),
									f.field( index.binding().field2.relativeFieldName, String.class ),
									f.field( index.binding().field3.relativeFieldName, LocalDate.class ) )
							.add( f.field( index.binding().field4.relativeFieldName, String.class ) )
			);
		}

		@Override
		protected Collection<List<?>> expectedLists() {
			return fourComponentExpectedLists();
		}
	}

	public static class SpecificsIT {
		@Test
		public void compositeInComposite() {
			assertThatQuery( index.query()
					.select( f -> f.composite()
							.add( f.composite()
									.add( f.field( index.binding().field1.relativeFieldName, String.class ) )
									.add( f.field( index.binding().field2.relativeFieldName, String.class ) )
									.transform( Pair::new ) )
							.add( f.score() )
							.transform( Pair::new ) )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder(
							new Pair<>( new Pair<>( index.binding().field1.document1Value.indexedValue, index.binding().field2.document1Value.indexedValue ), 1.0F ),
							new Pair<>( new Pair<>( index.binding().field1.document2Value.indexedValue, index.binding().field2.document2Value.indexedValue ), 1.0F ),
							new Pair<>( new Pair<>( index.binding().field1.document3Value.indexedValue, index.binding().field2.document3Value.indexedValue ), 1.0F )
					);
		}
	}


	private static class IndexBinding {
		final FieldModel<String> field1;
		final FieldModel<String> field2;
		final FieldModel<LocalDate> field3;
		final FieldModel<String> field4;

		IndexBinding(IndexSchemaElement root) {
			field1 = FieldModel.mapper( String.class, "field1value1", "field1value2", "field1value3" )
					.map( root, "field1" );
			field2 = FieldModel.mapper( String.class, "field2value1", "field2value2", "field2value3" )
					.map( root, "field2" );
			field3 = FieldModel.mapper( LocalDate.class,
							LocalDate.of( 2017, 12, 3 ),
							LocalDate.of( 2017, 12, 4 ),
							LocalDate.of( 2017, 12, 5 ) )
					.map( root, "field3" );
			field4 = FieldModel.mapper( String.class, "field4value1", "field4value2", "field4value3" )
					.map( root, "field4" );
		}
	}

	private static class ValueModel<F> {
		private final IndexFieldReference<F> reference;
		final F indexedValue;

		private ValueModel(IndexFieldReference<F> reference, F indexedValue) {
			this.reference = reference;
			this.indexedValue = indexedValue;
		}

		public void write(DocumentElement target) {
			target.addValue( reference, indexedValue );
		}
	}

	private static class FieldModel<F> {
		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(Class<F> type,
				F document1Value, F document2Value, F document3Value) {
			return mapper(
					c -> (StandardIndexFieldTypeOptionsStep<?, F>) c.as( type ),
					document1Value, document2Value, document3Value
			);
		}

		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> configuration,
				F document1Value, F document2Value, F document3Value) {
			return StandardFieldMapper.of(
					configuration,
					c -> c.projectable( Projectable.YES ),
					(reference, name) -> new FieldModel<>( reference, name, document1Value, document2Value, document3Value )
			);
		}

		final String relativeFieldName;

		final ValueModel<F> document1Value;
		final ValueModel<F> document2Value;
		final ValueModel<F> document3Value;

		private FieldModel(IndexFieldReference<F> reference, String relativeFieldName,
				F document1Value, F document2Value, F document3Value) {
			this.relativeFieldName = relativeFieldName;
			this.document1Value = new ValueModel<>( reference, document1Value );
			this.document2Value = new ValueModel<>( reference, document2Value );
			this.document3Value = new ValueModel<>( reference, document3Value );
		}
	}

	private static class Book {

		private String title;

		public Book(String title) {
			this.title = title;
		}

		@Override
		public boolean equals(Object obj) {
			if ( !(obj instanceof Book) ) {
				return false;
			}
			Book other = (Book) obj;
			return Objects.equals( title, other.title );
		}

		@Override
		public int hashCode() {
			return Objects.hash( title );
		}

		@Override
		public String toString() {
			return title;
		}
	}

}
