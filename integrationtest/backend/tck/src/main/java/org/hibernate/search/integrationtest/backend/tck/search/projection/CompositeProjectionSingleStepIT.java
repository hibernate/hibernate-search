/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests composite projections created through the single-step DSL,
 * e.g. {@code f.composite( MyPair::new, otherProjection1, otherProjection2 ) },
 * as opposed to the multi-step DSL,
 * e.g. {@code f.composite().from( otherProjection1, otherProjection2 ).as( MyPair::new ) },
 * which is tested in {@link AbstractCompositeProjectionFromAsIT}.
 */
@SuppressWarnings("deprecation")
class CompositeProjectionSingleStepIT {

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

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	void compositeList_fromSearchProjectionObjects() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> query = scope.query()
				.select( f -> f.composite(
						f.field( index.binding().author.relativeFieldName, String.class ).toProjection(),
						f.field( index.binding().title.relativeFieldName, String.class ).toProjection()
				)
				)
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder(
				Arrays.asList( index.binding().author.document1Value.indexedValue,
						index.binding().title.document1Value.indexedValue ),
				Arrays.asList( index.binding().author.document2Value.indexedValue,
						index.binding().title.document2Value.indexedValue ),
				Arrays.asList( index.binding().author.document3Value.indexedValue,
						index.binding().title.document3Value.indexedValue )
		);
	}

	@Test
	void compositeList_fromTerminalContexts() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> query = scope.query()
				.select( f -> f.composite(
						f.field( index.binding().author.relativeFieldName, String.class ),
						f.field( index.binding().title.relativeFieldName, String.class )
				)
				)
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder(
				Arrays.asList( index.binding().author.document1Value.indexedValue,
						index.binding().title.document1Value.indexedValue ),
				Arrays.asList( index.binding().author.document2Value.indexedValue,
						index.binding().title.document2Value.indexedValue ),
				Arrays.asList( index.binding().author.document3Value.indexedValue,
						index.binding().title.document3Value.indexedValue )
		);
	}

	@Test
	void compositeList_transformer_fromSearchProjectionObjects() {
		StubMappingScope scope = index.createScope();

		SearchQuery<Book_Bi> query = scope.query()
				.select( f -> f.composite(
						this::listToBook_Bi,
						f.field( index.binding().author.relativeFieldName, String.class ).toProjection(),
						f.field( index.binding().title.relativeFieldName, String.class ).toProjection()
				)
				)
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder(
				new Book_Bi( index.binding().author.document1Value.indexedValue,
						index.binding().title.document1Value.indexedValue ),
				new Book_Bi( index.binding().author.document2Value.indexedValue,
						index.binding().title.document2Value.indexedValue ),
				new Book_Bi( index.binding().author.document3Value.indexedValue,
						index.binding().title.document3Value.indexedValue )
		);
	}

	@Test
	void compositeList_transformer_fromTerminalContext() {
		StubMappingScope scope = index.createScope();

		SearchQuery<Book_Bi> query = scope.query()
				.select( f -> f.composite(
						this::listToBook_Bi,
						f.field( index.binding().author.relativeFieldName, String.class ),
						f.field( index.binding().title.relativeFieldName, String.class )
				)
				)
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder(
				new Book_Bi( index.binding().author.document1Value.indexedValue,
						index.binding().title.document1Value.indexedValue ),
				new Book_Bi( index.binding().author.document2Value.indexedValue,
						index.binding().title.document2Value.indexedValue ),
				new Book_Bi( index.binding().author.document3Value.indexedValue,
						index.binding().title.document3Value.indexedValue )
		);
	}

	@Test
	void compositeFunction_fromSearchProjectionObjects() {
		StubMappingScope scope = index.createScope();

		SearchQuery<Book> query = scope.query()
				.select( f -> f.composite(
						Book::new,
						f.field( index.binding().title.relativeFieldName, String.class ).toProjection()
				)
				)
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder(
				new Book( index.binding().title.document1Value.indexedValue ),
				new Book( index.binding().title.document2Value.indexedValue ),
				new Book( index.binding().title.document3Value.indexedValue )
		);
	}

	@Test
	void compositeFunction_fromTerminalContext() {
		StubMappingScope scope = index.createScope();

		SearchQuery<Book> query = scope.query()
				.select( f -> f.composite(
						Book::new,
						f.field( index.binding().title.relativeFieldName, String.class )
				)
				)
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder(
				new Book( index.binding().title.document1Value.indexedValue ),
				new Book( index.binding().title.document2Value.indexedValue ),
				new Book( index.binding().title.document3Value.indexedValue )
		);
	}

	@Test
	void compositeBiFunction_fromSearchProjectionObjects() {
		StubMappingScope scope = index.createScope();

		SearchQuery<Book_Bi> query = scope.query()
				.select( f -> f.composite(
						Book_Bi::new,
						f.field( index.binding().author.relativeFieldName, String.class ).toProjection(),
						f.field( index.binding().title.relativeFieldName, String.class ).toProjection()
				)
				)
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder(
				new Book_Bi( index.binding().author.document1Value.indexedValue,
						index.binding().title.document1Value.indexedValue ),
				new Book_Bi( index.binding().author.document2Value.indexedValue,
						index.binding().title.document2Value.indexedValue ),
				new Book_Bi( index.binding().author.document3Value.indexedValue,
						index.binding().title.document3Value.indexedValue )
		);
	}

	@Test
	void compositeBiFunction_fromTerminalContexts() {
		StubMappingScope scope = index.createScope();

		SearchQuery<Book_Bi> query = scope.query()
				.select( f -> f.composite(
						Book_Bi::new,
						f.field( index.binding().author.relativeFieldName, String.class ),
						f.field( index.binding().title.relativeFieldName, String.class )
				)
				)
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder(
				new Book_Bi( index.binding().author.document1Value.indexedValue,
						index.binding().title.document1Value.indexedValue ),
				new Book_Bi( index.binding().author.document2Value.indexedValue,
						index.binding().title.document2Value.indexedValue ),
				new Book_Bi( index.binding().author.document3Value.indexedValue,
						index.binding().title.document3Value.indexedValue )
		);
	}

	@Test
	void compositeTriFunction_fromSearchProjectionObjects() {
		StubMappingScope scope = index.createScope();

		SearchQuery<Book_Tri> query = scope.query()
				.select( f -> f.composite(
						Book_Tri::new,
						f.field( index.binding().author.relativeFieldName, String.class ).toProjection(),
						f.field( index.binding().title.relativeFieldName, String.class ).toProjection(),
						f.field( index.binding().releaseDate.relativeFieldName, LocalDate.class ).toProjection()
				)
				)
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder(
				new Book_Tri( index.binding().author.document1Value.indexedValue,
						index.binding().title.document1Value.indexedValue,
						index.binding().releaseDate.document1Value.indexedValue ),
				new Book_Tri( index.binding().author.document2Value.indexedValue,
						index.binding().title.document2Value.indexedValue,
						index.binding().releaseDate.document2Value.indexedValue ),
				new Book_Tri( index.binding().author.document3Value.indexedValue,
						index.binding().title.document3Value.indexedValue,
						index.binding().releaseDate.document3Value.indexedValue )
		);
	}

	@Test
	void compositeTriFunction_fromTerminalContexts() {
		StubMappingScope scope = index.createScope();

		SearchQuery<Book_Tri> query = scope.query()
				.select( f -> f.composite(
						Book_Tri::new,
						f.field( index.binding().author.relativeFieldName, String.class ),
						f.field( index.binding().title.relativeFieldName, String.class ),
						f.field( index.binding().releaseDate.relativeFieldName, LocalDate.class )
				)
				)
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder(
				new Book_Tri( index.binding().author.document1Value.indexedValue,
						index.binding().title.document1Value.indexedValue,
						index.binding().releaseDate.document1Value.indexedValue ),
				new Book_Tri( index.binding().author.document2Value.indexedValue,
						index.binding().title.document2Value.indexedValue,
						index.binding().releaseDate.document2Value.indexedValue ),
				new Book_Tri( index.binding().author.document3Value.indexedValue,
						index.binding().title.document3Value.indexedValue,
						index.binding().releaseDate.document3Value.indexedValue )
		);
	}

	@Test
	void nestedComposite() {
		StubMappingScope scope = index.createScope();

		SearchQuery<Book_Bi_Score> query = scope.query()
				.select( f -> f.composite(
						Book_Bi_Score::new,
						f.composite(
								Book_Bi::new,
								f.field( index.binding().author.relativeFieldName, String.class ),
								f.field( index.binding().title.relativeFieldName, String.class )
						),
						f.score()
				)
				)
				.where( f -> f.matchAll() )
				.toQuery();

		assertThatQuery( query ).hasHitsAnyOrder(
				new Book_Bi_Score( new Book_Bi( index.binding().author.document1Value.indexedValue,
						index.binding().title.document1Value.indexedValue ), 1.0F ),
				new Book_Bi_Score( new Book_Bi( index.binding().author.document2Value.indexedValue,
						index.binding().title.document2Value.indexedValue ), 1.0F ),
				new Book_Bi_Score( new Book_Bi( index.binding().author.document3Value.indexedValue,
						index.binding().title.document3Value.indexedValue ), 1.0F )
		);
	}

	private void initData() {
		index.bulkIndexer()
				.add( DOCUMENT_1, document -> {
					index.binding().author.document1Value.write( document );
					index.binding().title.document1Value.write( document );
					index.binding().releaseDate.document1Value.write( document );
				} )
				.add( DOCUMENT_2, document -> {
					index.binding().author.document2Value.write( document );
					index.binding().title.document2Value.write( document );
					index.binding().releaseDate.document2Value.write( document );
				} )
				.add( DOCUMENT_3, document -> {
					index.binding().author.document3Value.write( document );
					index.binding().title.document3Value.write( document );
					index.binding().releaseDate.document3Value.write( document );
				} )
				.join();
	}

	private static class IndexBinding {
		final FieldModel<String> author;
		final FieldModel<String> title;
		final FieldModel<LocalDate> releaseDate;

		IndexBinding(IndexSchemaElement root) {
			author = FieldModel.mapper( String.class, AUTHOR_4_3_2_1, AUTHOR_CIDER_HOUSE, AUTHOR_AVENUE_OF_MYSTERIES )
					.map( root, "author" );
			title = FieldModel.mapper( String.class, TITLE_4_3_2_1, TITLE_CIDER_HOUSE, TITLE_AVENUE_OF_MYSTERIES )
					.map( root, "title" );
			releaseDate = FieldModel
					.mapper( LocalDate.class, RELEASE_DATE_4_3_2_1, RELEASE_DATE_CIDER_HOUSE, RELEASE_DATE_AVENUE_OF_MYSTERIES )
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
		static <F> SimpleFieldMapper<F, ?, FieldModel<F>> mapper(Class<F> type,
				F document1Value, F document2Value, F document3Value) {
			return mapper(
					c -> (StandardIndexFieldTypeOptionsStep<?, F>) c.as( type ),
					document1Value, document2Value, document3Value
			);
		}

		static <F> SimpleFieldMapper<F, ?, FieldModel<F>> mapper(
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> configuration,
				F document1Value, F document2Value, F document3Value) {
			return SimpleFieldMapper.of(
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
			if ( !( obj instanceof Book ) ) {
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
			if ( !( obj instanceof Book_Bi ) ) {
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
			if ( !( obj instanceof Book_Tri ) ) {
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
			if ( !( obj instanceof Book_Bi_Score ) ) {
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
