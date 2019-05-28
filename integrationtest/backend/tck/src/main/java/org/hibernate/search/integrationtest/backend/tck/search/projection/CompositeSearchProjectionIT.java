/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CompositeSearchProjectionIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	private static final String TITLE_4_3_2_1 = "4 3 2 1";
	private static final String AUTHOR_4_3_2_1 = "Paul Auster";
	private static final LocalDate RELEASE_DATE_4_3_2_1 = LocalDate.of( 2017, 12, 3 );

	private static final String TITLE_CIDER_HOUSE = "The Cider House Rules";
	private static final String AUTHOR_CIDER_HOUSE = "John Irving";
	private static final LocalDate RELEASE_DATE_CIDER_HOUSE = LocalDate.of( 1985, 5, 4 );

	private static final String TITLE_AVENUE_OF_MYSTERIES = "Avenue of Mysteries";
	private static final String AUTHOR_AVENUE_OF_MYSTERIES = "John Irving";
	private static final LocalDate RELEASE_DATE_AVENUE_OF_MYSTERIES = LocalDate.of( 2015, 4, 7 );

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void compositeList_fromSearchProjectionObjects() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<List<?>> query = scope.query()
				.asProjection( f ->
						f.composite(
								f.field( indexMapping.author.relativeFieldName, String.class ).toProjection(),
								f.field( indexMapping.title.relativeFieldName, String.class ).toProjection()
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder(
				Arrays.asList( indexMapping.author.document1Value.indexedValue, indexMapping.title.document1Value.indexedValue ),
				Arrays.asList( indexMapping.author.document2Value.indexedValue, indexMapping.title.document2Value.indexedValue ),
				Arrays.asList( indexMapping.author.document3Value.indexedValue, indexMapping.title.document3Value.indexedValue )
		);
	}

	@Test
	public void compositeList_fromTerminalContexts() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<List<?>> query = scope.query()
				.asProjection( f ->
						f.composite(
								f.field( indexMapping.author.relativeFieldName, String.class ),
								f.field( indexMapping.title.relativeFieldName, String.class )
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder(
				Arrays.asList( indexMapping.author.document1Value.indexedValue, indexMapping.title.document1Value.indexedValue ),
				Arrays.asList( indexMapping.author.document2Value.indexedValue, indexMapping.title.document2Value.indexedValue ),
				Arrays.asList( indexMapping.author.document3Value.indexedValue, indexMapping.title.document3Value.indexedValue )
		);
	}

	@Test
	public void compositeList_transformer_fromSearchProjectionObjects() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Book_Bi> query = scope.query()
				.asProjection( f ->
						f.composite(
								this::listToBook_Bi,
								f.field( indexMapping.author.relativeFieldName, String.class ).toProjection(),
								f.field( indexMapping.title.relativeFieldName, String.class ).toProjection()
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder(
				new Book_Bi( indexMapping.author.document1Value.indexedValue, indexMapping.title.document1Value.indexedValue ),
				new Book_Bi( indexMapping.author.document2Value.indexedValue, indexMapping.title.document2Value.indexedValue ),
				new Book_Bi( indexMapping.author.document3Value.indexedValue, indexMapping.title.document3Value.indexedValue )
		);
	}

	@Test
	public void compositeList_transformer_fromTerminalContext() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Book_Bi> query = scope.query()
				.asProjection( f ->
						f.composite(
								this::listToBook_Bi,
								f.field( indexMapping.author.relativeFieldName, String.class ),
								f.field( indexMapping.title.relativeFieldName, String.class )
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder(
				new Book_Bi( indexMapping.author.document1Value.indexedValue, indexMapping.title.document1Value.indexedValue ),
				new Book_Bi( indexMapping.author.document2Value.indexedValue, indexMapping.title.document2Value.indexedValue ),
				new Book_Bi( indexMapping.author.document3Value.indexedValue, indexMapping.title.document3Value.indexedValue )
		);
	}

	@Test
	public void compositeFunction_fromSearchProjectionObjects() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Book> query = scope.query()
				.asProjection( f ->
						f.composite(
								Book::new,
								f.field( indexMapping.title.relativeFieldName, String.class ).toProjection()
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder(
				new Book( indexMapping.title.document1Value.indexedValue ),
				new Book( indexMapping.title.document2Value.indexedValue ),
				new Book( indexMapping.title.document3Value.indexedValue )
		);
	}

	@Test
	public void compositeFunction_fromTerminalContext() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Book> query = scope.query()
				.asProjection( f ->
						f.composite(
								Book::new,
								f.field( indexMapping.title.relativeFieldName, String.class )
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder(
				new Book( indexMapping.title.document1Value.indexedValue ),
				new Book( indexMapping.title.document2Value.indexedValue ),
				new Book( indexMapping.title.document3Value.indexedValue )
		);
	}

	@Test
	public void compositeBiFunction_fromSearchProjectionObjects() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Book_Bi> query = scope.query()
				.asProjection( f ->
						f.composite(
								Book_Bi::new,
								f.field( indexMapping.author.relativeFieldName, String.class ).toProjection(),
								f.field( indexMapping.title.relativeFieldName, String.class ).toProjection()
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder(
				new Book_Bi( indexMapping.author.document1Value.indexedValue, indexMapping.title.document1Value.indexedValue ),
				new Book_Bi( indexMapping.author.document2Value.indexedValue, indexMapping.title.document2Value.indexedValue ),
				new Book_Bi( indexMapping.author.document3Value.indexedValue, indexMapping.title.document3Value.indexedValue )
		);
	}

	@Test
	public void compositeBiFunction_fromTerminalContexts() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Book_Bi> query = scope.query()
				.asProjection( f ->
						f.composite(
								Book_Bi::new,
								f.field( indexMapping.author.relativeFieldName, String.class ),
								f.field( indexMapping.title.relativeFieldName, String.class )
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder(
				new Book_Bi( indexMapping.author.document1Value.indexedValue, indexMapping.title.document1Value.indexedValue ),
				new Book_Bi( indexMapping.author.document2Value.indexedValue, indexMapping.title.document2Value.indexedValue ),
				new Book_Bi( indexMapping.author.document3Value.indexedValue, indexMapping.title.document3Value.indexedValue )
		);
	}

	@Test
	public void compositeTriFunction_fromSearchProjectionObjects() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Book_Tri> query = scope.query()
				.asProjection( f ->
						f.composite(
								Book_Tri::new,
								f.field( indexMapping.author.relativeFieldName, String.class ).toProjection(),
								f.field( indexMapping.title.relativeFieldName, String.class ).toProjection(),
								f.field( indexMapping.releaseDate.relativeFieldName, LocalDate.class ).toProjection()
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder(
				new Book_Tri( indexMapping.author.document1Value.indexedValue, indexMapping.title.document1Value.indexedValue,
						indexMapping.releaseDate.document1Value.indexedValue ),
				new Book_Tri( indexMapping.author.document2Value.indexedValue, indexMapping.title.document2Value.indexedValue,
						indexMapping.releaseDate.document2Value.indexedValue ),
				new Book_Tri( indexMapping.author.document3Value.indexedValue, indexMapping.title.document3Value.indexedValue,
						indexMapping.releaseDate.document3Value.indexedValue )
		);
	}

	@Test
	public void compositeTriFunction_fromTerminalContexts() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Book_Tri> query = scope.query()
				.asProjection( f ->
						f.composite(
								Book_Tri::new,
								f.field( indexMapping.author.relativeFieldName, String.class ),
								f.field( indexMapping.title.relativeFieldName, String.class ),
								f.field( indexMapping.releaseDate.relativeFieldName, LocalDate.class )
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder(
				new Book_Tri( indexMapping.author.document1Value.indexedValue, indexMapping.title.document1Value.indexedValue,
						indexMapping.releaseDate.document1Value.indexedValue ),
				new Book_Tri( indexMapping.author.document2Value.indexedValue, indexMapping.title.document2Value.indexedValue,
						indexMapping.releaseDate.document2Value.indexedValue ),
				new Book_Tri( indexMapping.author.document3Value.indexedValue, indexMapping.title.document3Value.indexedValue,
						indexMapping.releaseDate.document3Value.indexedValue )
		);
	}

	@Test
	public void nestedComposite() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Book_Bi_Score> query = scope.query()
				.asProjection( f ->
						f.composite(
								Book_Bi_Score::new,
								f.composite(
										Book_Bi::new,
										f.field( indexMapping.author.relativeFieldName, String.class ),
										f.field( indexMapping.title.relativeFieldName, String.class )
								),
								f.score()
						)
				)
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasHitsAnyOrder(
				new Book_Bi_Score( new Book_Bi( indexMapping.author.document1Value.indexedValue, indexMapping.title.document1Value.indexedValue ), 1.0F ),
				new Book_Bi_Score( new Book_Bi( indexMapping.author.document2Value.indexedValue, indexMapping.title.document2Value.indexedValue ), 1.0F ),
				new Book_Bi_Score( new Book_Bi( indexMapping.author.document3Value.indexedValue, indexMapping.title.document3Value.indexedValue ), 1.0F )
		);
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexMapping.author.document1Value.write( document );
			indexMapping.title.document1Value.write( document );
			indexMapping.releaseDate.document1Value.write( document );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexMapping.author.document2Value.write( document );
			indexMapping.title.document2Value.write( document );
			indexMapping.releaseDate.document2Value.write( document );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexMapping.author.document3Value.write( document );
			indexMapping.title.document3Value.write( document );
			indexMapping.releaseDate.document3Value.write( document );
		} );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	private static class IndexMapping {
		final FieldModel<String> author;
		final FieldModel<String> title;
		final FieldModel<LocalDate> releaseDate;

		IndexMapping(IndexSchemaElement root) {
			author = FieldModel.mapper( String.class, AUTHOR_4_3_2_1, AUTHOR_CIDER_HOUSE, AUTHOR_AVENUE_OF_MYSTERIES )
					.map( root, "author" );
			title = FieldModel.mapper( String.class, TITLE_4_3_2_1, TITLE_CIDER_HOUSE, TITLE_AVENUE_OF_MYSTERIES )
					.map( root, "title" );
			releaseDate = FieldModel.mapper( LocalDate.class, RELEASE_DATE_4_3_2_1, RELEASE_DATE_CIDER_HOUSE, RELEASE_DATE_AVENUE_OF_MYSTERIES )
					.map( root, "releaseDate" );
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
					c -> (StandardIndexFieldTypeContext<?, F>) c.as( type ),
					document1Value, document2Value, document3Value
			);
		}

		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(
				Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, F>> configuration,
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

	private Book_Bi listToBook_Bi(List<?> elements) {
		return new Book_Bi( (String) elements.get( 0 ), (String) elements.get( 1 ) );
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

	private static class Book_Bi {

		private String author;

		private String title;

		public Book_Bi(String author, String title) {
			this.author = author;
			this.title = title;
		}

		@Override
		public boolean equals(Object obj) {
			if ( !(obj instanceof Book_Bi) ) {
				return false;
			}
			Book_Bi other = (Book_Bi) obj;
			return Objects.equals( author, other.author )
					&& Objects.equals( title, other.title );
		}

		@Override
		public int hashCode() {
			return Objects.hash( author, title );
		}

		@Override
		public String toString() {
			return author + " - " + title;
		}
	}

	private static class Book_Tri {

		private String author;

		private String title;

		private LocalDate releaseDate;

		public Book_Tri(String author, String title, LocalDate releaseDate) {
			this.author = author;
			this.title = title;
			this.releaseDate = releaseDate;
		}

		@Override
		public boolean equals(Object obj) {
			if ( !(obj instanceof Book_Tri) ) {
				return false;
			}
			Book_Tri other = (Book_Tri) obj;
			return Objects.equals( author, other.author )
					&& Objects.equals( title, other.title )
					&& Objects.equals( releaseDate, other.releaseDate );
		}

		@Override
		public int hashCode() {
			return Objects.hash( author, title, releaseDate );
		}

		@Override
		public String toString() {
			return author + " - " + title + " - " + releaseDate;
		}
	}

	private static class Book_Bi_Score {

		private Book_Bi book;

		private Float score;

		public Book_Bi_Score(Book_Bi book, Float score) {
			this.book = book;
			this.score = score;
		}

		@Override
		public boolean equals(Object obj) {
			if ( !(obj instanceof Book_Bi_Score) ) {
				return false;
			}
			Book_Bi_Score other = (Book_Bi_Score) obj;
			return Objects.equals( book, other.book )
					&& Objects.equals( score, other.score );
		}

		@Override
		public int hashCode() {
			return Objects.hash( book, score );
		}

		@Override
		public String toString() {
			return book + " - " + score;
		}
	}
}
