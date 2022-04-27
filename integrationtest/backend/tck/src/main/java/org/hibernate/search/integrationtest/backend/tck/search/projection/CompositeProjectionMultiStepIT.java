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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionAsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom1AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom2AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom3AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFromStep;
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

/**
 * Tests composite projections created through the multi-step DSL,
 * e.g. {@code f.composite().from( otherProjection1, otherProjection2 ).as( MyPair::new ) },
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

	private abstract static class AbstractFromAnyNumberIT {

		@Test
		public void asList() {
			assertThatQuery( index.query()
					.select( f -> doFrom( f, f.composite() ).asList() )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedLists() );
		}

		@Test
		public void asList_transformer() {
			assertThatQuery( index.query()
					.select( f -> doFrom( f, f.composite() ).asList( ValueWrapper<List<?>>::new ) )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedLists().stream().<ValueWrapper<List<?>>>map( ValueWrapper::new )
							.collect( Collectors.toList() ) );
		}

		@Test
		@TestForIssue(jiraKey = "HSEARCH-4553")
		public void asArray() {
			assertThatQuery( index.query()
					.select( f -> doFrom( f, f.composite() ).asArray() )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedLists().stream().map( list -> list.toArray() )
							.collect( Collectors.toList() ) );
		}

		@Test
		@TestForIssue(jiraKey = "HSEARCH-4553")
		public void asArray_transformer() {
			assertThatQuery( index.query()
					.select( f -> doFrom( f, f.composite() ).asArray( ValueWrapper<Object[]>::new ) )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedLists().stream().map( list -> list.toArray() )
							.<ValueWrapper<Object[]>>map( ValueWrapper::new )
							.collect( Collectors.toList() ) );
		}

		protected abstract CompositeProjectionAsStep doFrom(SearchProjectionFactory<?, ?> f,
				CompositeProjectionFromStep step);

		protected abstract Collection<List<?>> expectedLists();

	}

	private abstract static class AbstractFromSpecificNumberIT<S extends CompositeProjectionAsStep, T>
			extends AbstractFromAnyNumberIT {

		@Test
		public void as_transformer() {
			assertThatQuery( index.query()
					.select( f -> doAs( doFrom( f, f.composite() ) ) )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedTransformed() );
		}

		@Override
		protected abstract S doFrom(SearchProjectionFactory<?, ?> f, CompositeProjectionFromStep step);

		protected abstract ProjectionFinalStep<T> doAs(S step);

		protected abstract Collection<T> expectedTransformed();

	}

	public static class From1IT
			extends AbstractFromSpecificNumberIT<CompositeProjectionFrom1AsStep<String>, ValueWrapper<String>> {
		@Override
		protected CompositeProjectionFrom1AsStep<String> doFrom(SearchProjectionFactory<?, ?> f,
				CompositeProjectionFromStep step) {
			return step.from( f.field( index.binding().field1.relativeFieldName, String.class ) );
		}

		@Override
		protected ProjectionFinalStep<ValueWrapper<String>> doAs(CompositeProjectionFrom1AsStep<String> step) {
			return step.as( ValueWrapper<String>::new );
		}

		@Override
		protected Collection<List<?>> expectedLists() {
			return Arrays.asList(
					Collections.singletonList( index.binding().field1.document1Value.indexedValue ),
					Collections.singletonList( index.binding().field1.document2Value.indexedValue ),
					Collections.singletonList( index.binding().field1.document3Value.indexedValue )
			);
		}

		@Override
		protected Collection<ValueWrapper<String>> expectedTransformed() {
			return Arrays.asList(
					new ValueWrapper<>( index.binding().field1.document1Value.indexedValue ),
					new ValueWrapper<>( index.binding().field1.document2Value.indexedValue ),
					new ValueWrapper<>( index.binding().field1.document3Value.indexedValue )
			);
		}
	}

	public static class From2IT
			extends AbstractFromSpecificNumberIT<CompositeProjectionFrom2AsStep<String, String>, Pair<String, String>> {
		@Override
		protected CompositeProjectionFrom2AsStep<String, String> doFrom(SearchProjectionFactory<?, ?> f,
				CompositeProjectionFromStep step) {
			return step.from( f.field( index.binding().field1.relativeFieldName, String.class ),
					f.field( index.binding().field2.relativeFieldName, String.class ) );
		}

		@Override
		protected ProjectionFinalStep<Pair<String, String>> doAs(CompositeProjectionFrom2AsStep<String, String> step) {
			return step.as( Pair::new );
		}

		@Override
		protected Collection<List<?>> expectedLists() {
			return Arrays.asList(
					Arrays.asList( index.binding().field1.document1Value.indexedValue,
							index.binding().field2.document1Value.indexedValue ),
					Arrays.asList( index.binding().field1.document2Value.indexedValue,
							index.binding().field2.document2Value.indexedValue ),
					Arrays.asList( index.binding().field1.document3Value.indexedValue,
							index.binding().field2.document3Value.indexedValue )
			);
		}

		@Override
		protected Collection<Pair<String, String>> expectedTransformed() {
			return Arrays.asList(
					new Pair<>( index.binding().field1.document1Value.indexedValue,
							index.binding().field2.document1Value.indexedValue ),
					new Pair<>( index.binding().field1.document2Value.indexedValue,
							index.binding().field2.document2Value.indexedValue ),
					new Pair<>( index.binding().field1.document3Value.indexedValue,
							index.binding().field2.document3Value.indexedValue )
			);
		}
	}

	public static class From3IT
			extends AbstractFromSpecificNumberIT<CompositeProjectionFrom3AsStep<String, String, LocalDate>, Triplet<String, String, LocalDate>> {
		@Override
		protected CompositeProjectionFrom3AsStep<String, String, LocalDate> doFrom(SearchProjectionFactory<?, ?> f,
				CompositeProjectionFromStep step) {
			return step.from( f.field( index.binding().field1.relativeFieldName, String.class ),
					f.field( index.binding().field2.relativeFieldName, String.class ),
					f.field( index.binding().field3.relativeFieldName, LocalDate.class ) );
		}

		@Override
		protected ProjectionFinalStep<Triplet<String, String, LocalDate>> doAs(
				CompositeProjectionFrom3AsStep<String, String, LocalDate> step) {
			return step.as( Triplet::new );
		}

		@Override
		protected Collection<List<?>> expectedLists() {
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

		@Override
		protected Collection<Triplet<String, String, LocalDate>> expectedTransformed() {
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
	}

	public static class From4IT
			extends AbstractFromAnyNumberIT {
		@Override
		protected CompositeProjectionAsStep doFrom(SearchProjectionFactory<?, ?> f, CompositeProjectionFromStep step) {
			return step.from( f.field( index.binding().field1.relativeFieldName, String.class ),
					f.field( index.binding().field2.relativeFieldName, String.class ),
					f.field( index.binding().field3.relativeFieldName, LocalDate.class ),
					f.field( index.binding().field4.relativeFieldName, String.class ) );
		}

		@Override
		protected Collection<List<?>> expectedLists() {
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
	}

	public static class SpecificsIT {
		@Test
		public void compositeInComposite() {
			assertThatQuery( index.query()
					.select( f -> f.composite().from(
							f.composite().from(
									f.field( index.binding().field1.relativeFieldName, String.class ),
									f.field( index.binding().field2.relativeFieldName, String.class )
							).as( Pair::new ),
							f.score()
					).as( Pair::new ) )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder(
							new Pair<>( new Pair<>( index.binding().field1.document1Value.indexedValue,
									index.binding().field2.document1Value.indexedValue ), 1.0F ),
							new Pair<>( new Pair<>( index.binding().field1.document2Value.indexedValue,
									index.binding().field2.document2Value.indexedValue ), 1.0F ),
							new Pair<>( new Pair<>( index.binding().field1.document3Value.indexedValue,
									index.binding().field2.document3Value.indexedValue ), 1.0F )
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

}
