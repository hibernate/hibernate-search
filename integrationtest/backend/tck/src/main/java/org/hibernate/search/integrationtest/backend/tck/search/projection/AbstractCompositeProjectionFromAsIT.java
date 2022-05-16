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
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionAsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom1AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom2AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom3AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFromStep;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
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
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Abstract base for tests of the from/as syntax of composite or object projections,
 * e.g. {@code f.composite().from( otherProjection1, otherProjection2 ).as( MyPair::new ) }.
 */
@RunWith(NestedRunner.class)
public abstract class AbstractCompositeProjectionFromAsIT<B extends AbstractCompositeProjectionFromAsIT.AbstractIndexBinding> {

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();
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

	protected abstract CompositeProjectionFromStep startProjection(SearchProjectionFactory<?, ?> f);

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

		protected abstract CompositeProjectionAsStep doFrom(SearchProjectionFactory<?, ?> f,
				CompositeProjectionFromStep step);

		protected final Collection<List<?>> expectedLists() {
			return dataSet.forEachDocument( this::expectedList );
		}

		protected abstract List<?> expectedList(int docOrdinal);

		protected final Collection<Object[]> expectedArrays() {
			return dataSet.forEachDocument( this::expectedArray );
		}

		protected final Object[] expectedArray(int docOrdinal) {
			return expectedList( docOrdinal ).toArray();
		}

	}

	private abstract class AbstractFromSpecificNumberIT<S extends CompositeProjectionAsStep, T>
			extends AbstractFromAnyNumberIT {

		@Test
		public void as_transformer() {
			assertThatQuery( index.query()
					.select( f -> doAs( doFrom( f, startProjection( f ) ) ) )
					.where( f -> f.matchAll() ) )
					.hasHitsAnyOrder( expectedTransformed() );
		}

		@Override
		protected abstract S doFrom(SearchProjectionFactory<?, ?> f, CompositeProjectionFromStep step);

		protected abstract ProjectionFinalStep<T> doAs(S step);

		protected final Collection<T> expectedTransformed() {
			return dataSet.forEachDocument( this::expectedTransformed );
		}

		protected abstract T expectedTransformed(int docOrdinal);

	}

	@Nested
	public class From1IT
			extends AbstractFromSpecificNumberIT<CompositeProjectionFrom1AsStep<String>, ValueWrapper<String>> {
		@Override
		protected CompositeProjectionFrom1AsStep<String> doFrom(SearchProjectionFactory<?, ?> f,
				CompositeProjectionFromStep step) {
			return step.from( f.field( index.binding().composite().field1Path(), String.class ) );
		}

		@Override
		protected ProjectionFinalStep<ValueWrapper<String>> doAs(CompositeProjectionFrom1AsStep<String> step) {
			return step.as( ValueWrapper<String>::new );
		}

		@Override
		protected List<?> expectedList(int docOrdinal) {
			return Arrays.asList( dataSet.field1Value( docOrdinal ) );
		}

		@Override
		protected ValueWrapper<String> expectedTransformed(int docOrdinal) {
			return new ValueWrapper<>( dataSet.field1Value( docOrdinal ) );
		}
	}

	@Nested
	public class From2IT
			extends AbstractFromSpecificNumberIT<CompositeProjectionFrom2AsStep<String, String>, Pair<String, String>> {
		@Override
		protected CompositeProjectionFrom2AsStep<String, String> doFrom(SearchProjectionFactory<?, ?> f,
				CompositeProjectionFromStep step) {
			return step.from( f.field( index.binding().composite().field1Path(), String.class ),
					f.field( index.binding().composite().field2Path(), String.class ) );
		}

		@Override
		protected ProjectionFinalStep<Pair<String, String>> doAs(CompositeProjectionFrom2AsStep<String, String> step) {
			return step.as( Pair::new );
		}

		@Override
		protected List<?> expectedList(int docOrdinal) {
			return Arrays.asList( dataSet.field1Value( docOrdinal ), dataSet.field2Value( docOrdinal ) );
		}

		@Override
		protected Pair<String, String> expectedTransformed(int docOrdinal) {
			return new Pair<>( dataSet.field1Value( docOrdinal ), dataSet.field2Value( docOrdinal ) );
		}
	}

	@Nested
	public class From3IT
			extends AbstractFromSpecificNumberIT<CompositeProjectionFrom3AsStep<String, String, LocalDate>, Triplet<String, String, LocalDate>> {
		@Override
		protected CompositeProjectionFrom3AsStep<String, String, LocalDate> doFrom(SearchProjectionFactory<?, ?> f,
				CompositeProjectionFromStep step) {
			return step.from( f.field( index.binding().composite().field1Path(), String.class ),
					f.field( index.binding().composite().field2Path(), String.class ),
					f.field( index.binding().composite().field3Path(), LocalDate.class ) );
		}

		@Override
		protected ProjectionFinalStep<Triplet<String, String, LocalDate>> doAs(
				CompositeProjectionFrom3AsStep<String, String, LocalDate> step) {
			return step.as( Triplet::new );
		}

		@Override
		protected List<?> expectedList(int docOrdinal) {
			return Arrays.asList( dataSet.field1Value( docOrdinal ), dataSet.field2Value( docOrdinal ),
					dataSet.field3Value( docOrdinal ) );
		}

		@Override
		protected Triplet<String, String, LocalDate> expectedTransformed(int docOrdinal) {
			return new Triplet<>( dataSet.field1Value( docOrdinal ), dataSet.field2Value( docOrdinal ),
					dataSet.field3Value( docOrdinal ) );
		}
	}

	@Nested
	public class From4IT
			extends AbstractFromAnyNumberIT {
		@Override
		protected CompositeProjectionAsStep doFrom(SearchProjectionFactory<?, ?> f, CompositeProjectionFromStep step) {
			return step.from( f.field( index.binding().composite().field1Path(), String.class ),
					f.field( index.binding().composite().field2Path(), String.class ),
					f.field( index.binding().composite().field3Path(), LocalDate.class ),
					f.field( index.binding().composite().field4Path(), String.class ) );
		}

		@Override
		protected List<?> expectedList(int docOrdinal) {
			return Arrays.asList( dataSet.field1Value( docOrdinal ), dataSet.field2Value( docOrdinal ),
							dataSet.field3Value( docOrdinal ), dataSet.field4Value( docOrdinal ) );
		}
	}

	protected abstract static class AbstractIndexBinding {

		abstract CompositeBinding composite();

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

		public String field1Path() {
			return FieldPaths.compose( absolutePath, field1.relativeFieldName );
		}

		public String field2Path() {
			return FieldPaths.compose( absolutePath, field2.relativeFieldName );
		}

		public String field3Path() {
			return FieldPaths.compose( absolutePath, field3.relativeFieldName );
		}

		public String field4Path() {
			return FieldPaths.compose( absolutePath, field4.relativeFieldName );
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

		public String field1Value(int docOrdinal) {
			return stringValues.fieldValue( 10 * docOrdinal + 1 );
		}

		public String field2Value(int docOrdinal) {
			return stringValues.fieldValue( 10 * docOrdinal + 2 );
		}

		public LocalDate field3Value(int docOrdinal) {
			return localDateValues.fieldValue( 10 * docOrdinal + 3 );
		}

		public String field4Value(int docOrdinal) {
			return stringValues.fieldValue( 10 * docOrdinal + 4 );
		}
	}

}
