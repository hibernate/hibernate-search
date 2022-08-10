/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.projection.definition.spi.ProjectionRegistry;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFromAsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom1AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom2AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom3AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.LocalDateFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.data.Pair;
import org.hibernate.search.util.impl.test.data.Triplet;
import org.hibernate.search.util.impl.test.runner.nested.Nested;
import org.hibernate.search.util.impl.test.runner.nested.NestedRunner;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

/**
 * Abstract base for tests of the from/as syntax of composite or object projections,
 * e.g. {@code f.composite().from( otherProjection1, otherProjection2 ).as( MyPair::new ) }.
 */
@RunWith(NestedRunner.class)
public abstract class AbstractCompositeProjectionFromAsIT<B extends AbstractCompositeProjectionFromAsIT.AbstractIndexBinding> {

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final ProjectionRegistry projectionRegistryMock = Mockito.mock( ProjectionRegistry.class );

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	private final SimpleMappedIndex<B> index;
	private final AbstractDataSet<B> dataSet;

	public AbstractCompositeProjectionFromAsIT(SimpleMappedIndex<B> index, AbstractDataSet<B> dataSet) {
		this.index = index;
		this.dataSet = dataSet;
	}

	@Test
	public void takariCpSuiteWorkaround() {
		// Workaround to get Takari-CPSuite to run this test.
	}

	protected abstract CompositeProjectionInnerStep startProjection(SearchProjectionFactory<?, ?> f);

	protected abstract CompositeProjectionInnerStep startProjectionForMulti(SearchProjectionFactory<?, ?> f);

	private abstract class AbstractFromAnyNumberIT {

		@Test
		public void asList() {
			assertThatQuery( index.query()
					.select( f -> doFrom( f, startProjection( f ) ).asList() )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedLists() );
		}

		@Test
		public void asList_transformer() {
			assertThatQuery( index.query()
					.select( f -> doFrom( f, startProjection( f ) ).asList( ValueWrapper<List<?>>::new ) )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedLists().stream().<ValueWrapper<List<?>>>map( ValueWrapper::new )
							.collect( Collectors.toList() ) );
		}

		@Test
		@TestForIssue(jiraKey = "HSEARCH-4553")
		// The most important check here is at compile time: if this compiles,
		// then we're being flexible enough regarding function argument types.
		public void asList_transformer_flexibleFunctionArgumentTypes() {
			assertThatQuery( index.query()
					.select( f -> doFrom( f, startProjection( f ) ).asList( (Function<? super List<?>, Object>) ValueWrapper<List<?>>::new ) )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedLists().stream().<ValueWrapper<List<?>>>map( ValueWrapper::new )
							.collect( Collectors.toList() ) );
		}

		@Test
		public void asList_multi() {
			assertThatQuery( index.query()
					.select( f -> doFromForMulti( f, startProjectionForMulti( f ) ).asList().multi() )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedListsForMulti() );
		}

		@Test
		@TestForIssue(jiraKey = "HSEARCH-4553")
		public void asArray() {
			assertThatQuery( index.query()
					.select( f -> doFrom( f, startProjection( f ) ).asArray() )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedArrays() );
		}

		@Test
		@TestForIssue(jiraKey = "HSEARCH-4553")
		public void asArray_transformer() {
			assertThatQuery( index.query()
					.select( f -> doFrom( f, startProjection( f ) ).asArray( ValueWrapper<Object[]>::new ) )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedArrays().stream()
							.<ValueWrapper<Object[]>>map( ValueWrapper::new )
							.collect( Collectors.toList() ) );
		}

		@Test
		@TestForIssue(jiraKey = "HSEARCH-4553")
		// The most important check here is at compile time: if this compiles,
		// then we're being flexible enough regarding function argument types.
		public void asArray_transformer_flexibleFunctionArgumentTypes() {
			assertThatQuery( index.query()
					.select( f -> doFrom( f, startProjection( f ) ).asArray( (Function<? super Object[], Object>) ValueWrapper<Object[]>::new ) )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedArrays().stream()
							.<ValueWrapper<Object[]>>map( ValueWrapper::new )
							.collect( Collectors.toList() ) );
		}

		@Test
		public void asArray_multi() {
			assertThatQuery( index.query()
					.select( f -> doFromForMulti( f, startProjectionForMulti( f ) ).asArray().multi() )
					.where( f -> f.matchAll() ) )
					.hits().asIs().usingRecursiveFieldByFieldElementComparator()
					.containsExactlyInAnyOrderElementsOf( expectedArraysForMulti() );
		}

		@Test
		@TestForIssue(jiraKey = "HSEARCH-3927")
		public void as_class() {
			index.mapping().with().projectionRegistry( projectionRegistryMock ).run( () -> {
				// We simulate a projection definition on the mapper side;
				// normally this would involve annotation mapping.
				when( projectionRegistryMock.composite( ValueWrapper.class ) )
						.thenReturn( (f, initialStep) ->
								// Critically, in a real-world scenario the inner projections
								// will be defined relative to the composite node
								// (which may not be the root in the case of object projections).
								// We need to do the same here, to check that the engine/backend compensates
								// by passing a projection factory whose root is the composite node.
								doFrom( f, index.binding().composite(), CompositeBinding::relativePath, initialStep )
										.asArray( ValueWrapper<Object[]>::new ) );
				assertThatQuery( index.createScope().query()
						.select( f -> startProjection( f ).as( ValueWrapper.class ) )
						.where( f -> f.matchAll() ) )
						.hasHitsAnyOrder( expectedArrays().stream()
								.<ValueWrapper<Object[]>>map( ValueWrapper::new )
								.collect( Collectors.toList() ) );
			} );
		}

		@Test
		@TestForIssue(jiraKey = "HSEARCH-3927")
		@SuppressWarnings("rawtypes")
		public void as_class_multi() {
			index.mapping().with().projectionRegistry( projectionRegistryMock ).run( () -> {
				// We simulate a projection definition on the mapper side;
				// normally this would involve annotation mapping.
				when( projectionRegistryMock.composite( ValueWrapper.class ) )
						.thenReturn( (f, initialStep) ->
								// Inner projections need to be defined relative to the composite node;
								// see as_class.
								doFrom( f, index.binding().compositeForMulti(), CompositeBinding::relativePath, initialStep )
										.asArray( ValueWrapper<Object[]>::new ) );
				assertThatQuery( index.createScope().query()
						.select( f -> startProjectionForMulti( f ).as( ValueWrapper.class ).multi() )
						.where( f -> f.matchAll() ) )
						.hits().asIs().usingRecursiveFieldByFieldElementComparator()
						.containsExactlyInAnyOrderElementsOf( expectedArraysForMulti().stream()
								.map( perDocList -> perDocList.stream()
										.<ValueWrapper>map( ValueWrapper::new )
										.collect( Collectors.toList() ) )
								.collect( Collectors.toList() ) );
			} );
		}

		@Test
		public void inComposite() {
			assertThatQuery( index.query()
					.select( f -> f.composite().from(
							doFrom( f, startProjection( f ) ).asList( ValueWrapper<List<?>>::new ),
							f.score()
					).as( Pair::new ) )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedLists().stream()
							.<ValueWrapper<List<?>>>map( ValueWrapper::new )
							.<Pair<ValueWrapper<List<?>>, Float>>map( valueWrapper -> new Pair<>( valueWrapper, 1.0f ) )
							.collect( Collectors.toList() ) );
		}

		protected CompositeProjectionFromAsStep doFrom(SearchProjectionFactory<?, ?> f,
				CompositeProjectionInnerStep step) {
			return doFrom( f, index.binding().composite(), CompositeBinding::absolutePath, step );
		}

		protected CompositeProjectionFromAsStep doFromForMulti(SearchProjectionFactory<?, ?> f,
				CompositeProjectionInnerStep step) {
			return doFrom( f, index.binding().compositeForMulti(), CompositeBinding::absolutePath, step );
		}

		protected abstract CompositeProjectionFromAsStep doFrom(SearchProjectionFactory<?, ?> f, CompositeBinding binding,
				BiFunction<CompositeBinding, SimpleFieldModel<?>, String> pathGetter,
				CompositeProjectionInnerStep step);

		protected final Collection<List<?>> expectedLists() {
			return dataSet.forEachDocument( docOrdinal -> expectedList( docOrdinal, 0 ) );
		}

		protected abstract List<?> expectedList(int docOrdinal, int inDocOrdinal);

		protected final Collection<List<List<?>>> expectedListsForMulti() {
			return dataSet.forEachDocumentAndObject( this::expectedList );
		}

		protected final Collection<Object[]> expectedArrays() {
			return dataSet.forEachDocument( docOrdinal -> expectedArray( docOrdinal, 0 ) );
		}

		protected final Collection<List<Object[]>> expectedArraysForMulti() {
			return dataSet.forEachDocumentAndObject( this::expectedArray );
		}

		protected final Object[] expectedArray(int docOrdinal, int inDocOrdinal) {
			return expectedList( docOrdinal, inDocOrdinal ).toArray();
		}

	}

	private abstract class AbstractFromSpecificNumberIT<S extends CompositeProjectionFromAsStep, T>
			extends AbstractFromAnyNumberIT {

		@Test
		public void as_transformer() {
			assertThatQuery( index.query()
					.select( f -> doAs( doFrom( f, startProjection( f ) ) ) )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedTransformed() );
		}

		@Test
		public void as_transformer_multi() {
			assertThatQuery( index.query()
					.select( f -> doAs( doFromForMulti( f, startProjectionForMulti( f ) ) ).multi() )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedTransformedForMulti() );
		}

		@Override
		protected final S doFrom(SearchProjectionFactory<?, ?> f, CompositeProjectionInnerStep step) {
			return doFrom( f, index.binding().composite(), CompositeBinding::absolutePath, step );
		}

		@Override
		protected final S doFromForMulti(SearchProjectionFactory<?, ?> f, CompositeProjectionInnerStep step) {
			return doFrom( f, index.binding().compositeForMulti(), CompositeBinding::absolutePath, step );
		}

		@Override
		protected abstract S doFrom(SearchProjectionFactory<?, ?> f, CompositeBinding binding,
				BiFunction<CompositeBinding, SimpleFieldModel<?>, String> pathGetter,
				CompositeProjectionInnerStep step);

		protected abstract CompositeProjectionValueStep<?, T> doAs(S step);

		protected final Collection<T> expectedTransformed() {
			return dataSet.forEachDocument( docOrdinal -> expectedTransformed( docOrdinal, 0 ) );
		}

		protected final Collection<List<T>> expectedTransformedForMulti() {
			return dataSet.forEachDocumentAndObject( this::expectedTransformed );
		}

		protected abstract T expectedTransformed(int docOrdinal, int inDocOrdinal);

	}

	@Nested
	public class From1IT
			extends AbstractFromSpecificNumberIT<CompositeProjectionFrom1AsStep<String>, ValueWrapper<String>> {
		@Override
		protected CompositeProjectionFrom1AsStep<String> doFrom(SearchProjectionFactory<?, ?> f,
				CompositeBinding binding, BiFunction<CompositeBinding, SimpleFieldModel<?>, String> pathGetter,
				CompositeProjectionInnerStep step) {
			return step.from( f.field( pathGetter.apply( binding, binding.field1 ), String.class ) );
		}

		@Override
		protected CompositeProjectionValueStep<?, ValueWrapper<String>> doAs(CompositeProjectionFrom1AsStep<String> step) {
			return step.as( ValueWrapper<String>::new );
		}

		@Override
		protected List<?> expectedList(int docOrdinal, int inDocOrdinal) {
			return Arrays.asList( dataSet.field1Value( docOrdinal, inDocOrdinal ) );
		}

		@Override
		protected ValueWrapper<String> expectedTransformed(int docOrdinal, int inDocOrdinal) {
			return new ValueWrapper<>( dataSet.field1Value( docOrdinal, inDocOrdinal ) );
		}
	}

	@Nested
	public class From2IT
			extends AbstractFromSpecificNumberIT<CompositeProjectionFrom2AsStep<String, String>, Pair<String, String>> {
		@Override
		protected CompositeProjectionFrom2AsStep<String, String> doFrom(SearchProjectionFactory<?, ?> f,
				CompositeBinding binding, BiFunction<CompositeBinding, SimpleFieldModel<?>, String> pathGetter,
				CompositeProjectionInnerStep step) {
			return step.from( f.field( pathGetter.apply( binding, binding.field1 ), String.class ),
					f.field( pathGetter.apply( binding, binding.field2 ), String.class ) );
		}

		@Override
		protected CompositeProjectionValueStep<?, Pair<String, String>> doAs(CompositeProjectionFrom2AsStep<String, String> step) {
			return step.as( Pair::new );
		}

		@Override
		protected List<?> expectedList(int docOrdinal, int inDocOrdinal) {
			return Arrays.asList( dataSet.field1Value( docOrdinal, inDocOrdinal ), dataSet.field2Value( docOrdinal, inDocOrdinal ) );
		}

		@Override
		protected Pair<String, String> expectedTransformed(int docOrdinal, int inDocOrdinal) {
			return new Pair<>( dataSet.field1Value( docOrdinal, inDocOrdinal ), dataSet.field2Value( docOrdinal, inDocOrdinal ) );
		}
	}

	@Nested
	public class From3IT
			extends AbstractFromSpecificNumberIT<CompositeProjectionFrom3AsStep<String, String, LocalDate>, Triplet<String, String, LocalDate>> {
		@Override
		protected CompositeProjectionFrom3AsStep<String, String, LocalDate> doFrom(SearchProjectionFactory<?, ?> f,
				CompositeBinding binding, BiFunction<CompositeBinding, SimpleFieldModel<?>, String> pathGetter,
				CompositeProjectionInnerStep step) {
			return step.from( f.field( pathGetter.apply( binding, binding.field1 ), String.class ),
					f.field( pathGetter.apply( binding, binding.field2 ), String.class ),
					f.field( pathGetter.apply( binding, binding.field3 ), LocalDate.class ) );
		}

		@Override
		protected CompositeProjectionValueStep<?, Triplet<String, String, LocalDate>> doAs(
				CompositeProjectionFrom3AsStep<String, String, LocalDate> step) {
			return step.as( Triplet::new );
		}

		@Override
		protected List<?> expectedList(int docOrdinal, int inDocOrdinal) {
			return Arrays.asList( dataSet.field1Value( docOrdinal, inDocOrdinal ),
					dataSet.field2Value( docOrdinal, inDocOrdinal ),
					dataSet.field3Value( docOrdinal, inDocOrdinal ) );
		}

		@Override
		protected Triplet<String, String, LocalDate> expectedTransformed(int docOrdinal, int inDocOrdinal) {
			return new Triplet<>( dataSet.field1Value( docOrdinal, inDocOrdinal ),
					dataSet.field2Value( docOrdinal, inDocOrdinal ),
					dataSet.field3Value( docOrdinal, inDocOrdinal ) );
		}
	}

	@Nested
	public class From4IT
			extends AbstractFromAnyNumberIT {
		@Override
		protected CompositeProjectionFromAsStep doFrom(SearchProjectionFactory<?, ?> f,
				CompositeBinding binding, BiFunction<CompositeBinding, SimpleFieldModel<?>, String> pathGetter,
				CompositeProjectionInnerStep step) {
			return step.from( f.field( pathGetter.apply( binding, binding.field1 ), String.class ),
					f.field( pathGetter.apply( binding, binding.field2 ), String.class ),
					f.field( pathGetter.apply( binding, binding.field3 ), LocalDate.class ),
					f.field( pathGetter.apply( binding, binding.field4 ), String.class ) );
		}

		@Override
		protected List<?> expectedList(int docOrdinal, int inDocOrdinal) {
			return Arrays.asList( dataSet.field1Value( docOrdinal, inDocOrdinal ),
					dataSet.field2Value( docOrdinal, inDocOrdinal ),
					dataSet.field3Value( docOrdinal, inDocOrdinal ),
					dataSet.field4Value( docOrdinal, inDocOrdinal ) );
		}
	}

	protected abstract static class AbstractIndexBinding {

		abstract CompositeBinding composite();

		abstract CompositeBinding compositeForMulti();

	}

	static class CompositeBinding {
		final String absolutePath;
		final SimpleFieldModel<String> field1;
		final SimpleFieldModel<String> field2;
		final SimpleFieldModel<LocalDate> field3;
		final SimpleFieldModel<String> field4;

		CompositeBinding(IndexSchemaElement parent, String absolutePath) {
			this.absolutePath = absolutePath;
			field1 = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE,
							c -> c.projectable( Projectable.YES ) )
					.map( parent, "field1" );
			field2 = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE,
							c -> c.projectable( Projectable.YES ) )
					.map( parent, "field2" );
			field3 = SimpleFieldModel.mapper( LocalDateFieldTypeDescriptor.INSTANCE,
							c -> c.projectable( Projectable.YES ) )
					.map( parent, "field3" );
			field4 = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE,
							c -> c.projectable( Projectable.YES ) )
					.map( parent, "field4" );
		}

		public final String relativePath(SimpleFieldModel<?> fieldModel) {
			return fieldModel.relativeFieldName;
		}

		public final String absolutePath(SimpleFieldModel<?> fieldModel) {
			return FieldPaths.compose( absolutePath, fieldModel.relativeFieldName );
		}

	}

	public abstract static class AbstractDataSet<B extends AbstractIndexBinding> {
		private final FieldProjectionTestValues<String> stringValues = new FieldProjectionTestValues<>( KeywordStringFieldTypeDescriptor.INSTANCE );
		private final FieldProjectionTestValues<LocalDate> localDateValues = new FieldProjectionTestValues<>( LocalDateFieldTypeDescriptor.INSTANCE );

		public void contribute(SimpleMappedIndex<B> index, BulkIndexer indexer) {
			indexer
					.add( "0", document -> initDocument( index.binding(), 0, document ) )
					.add( "1", document -> initDocument( index.binding(), 1, document ) )
					.add( "2", document -> initDocument( index.binding(), 2, document ) );
		}

		abstract void initDocument(B binding, int docOrdinal, DocumentElement document);

		public <T> List<T> forEachDocument(IntFunction<T> function) {
			return Arrays.asList( function.apply( 0 ), function.apply( 1 ), function.apply( 2 ) );
		}

		public <T> List<List<T>> forEachDocumentAndObject(BiFunction<Integer, Integer, T> function) {
			return forEachDocument( docOrdinal -> forEachObjectInDocument( inDocOrdinal ->
					function.apply( docOrdinal, inDocOrdinal ) ) );
		}

		abstract <T> List<T> forEachObjectInDocument(IntFunction<T> function);

		public String field1Value(int docOrdinal) {
			return field1Value( docOrdinal, 0 );
		}

		public String field2Value(int docOrdinal) {
			return field2Value( docOrdinal, 0 );
		}

		public LocalDate field3Value(int docOrdinal) {
			return field3Value( docOrdinal, 0 );
		}

		public String field4Value(int docOrdinal) {
			return field4Value( docOrdinal, 0 );
		}

		public String field1Value(int docOrdinal, int inDocOrdinal) {
			return stringValues.fieldValue( 100 * docOrdinal + 10 * inDocOrdinal + 1 );
		}

		public String field2Value(int docOrdinal, int inDocOrdinal) {
			return stringValues.fieldValue( 100 * docOrdinal + 10 * inDocOrdinal + 2 );
		}

		public LocalDate field3Value(int docOrdinal, int inDocOrdinal) {
			return localDateValues.fieldValue( 100 * docOrdinal + 10 * inDocOrdinal + 3 );
		}

		public String field4Value(int docOrdinal, int inDocOrdinal) {
			return stringValues.fieldValue( 100 * docOrdinal + 10 * inDocOrdinal + 4 );
		}
	}

}
