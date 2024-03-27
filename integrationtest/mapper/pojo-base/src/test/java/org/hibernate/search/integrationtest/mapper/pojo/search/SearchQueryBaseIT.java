/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test everything related to the search query itself.
 * <p>
 * Does not test sorts and predicates, or other features that only involve the backend.
 * Those should be tested in the backend integration tests.
 */
class SearchQueryBaseIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@BeforeEach
	void setup() {
		backendMock.expectAnySchema( Book.NAME );
		backendMock.expectAnySchema( Author.NAME );

		mapping = setupHelper.start()
				.withAnnotatedTypes( Book.class, Author.class, NotIndexed.class )
				.setup();
	}

	@Test
	void target_byClass_singleType() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchQuery<EntityReference> query = searchSession.search( Book.class )
					.selectEntityReference()
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( Book.NAME ),
					b -> {},
					StubSearchWorkBehavior.of(
							3L,
							reference( Book.NAME, "1" ),
							reference( Book.NAME, "2" ),
							reference( Book.NAME, "3" )
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					PojoEntityReference.withName( Book.class, Book.NAME, 1 ),
					PojoEntityReference.withName( Book.class, Book.NAME, 2 ),
					PojoEntityReference.withName( Book.class, Book.NAME, 3 )
			);
		}
	}

	@Test
	void target_byClass_multipleTypes() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchQuery<EntityReference> query = searchSession.search( Arrays.asList( Book.class, Author.class ) )
					.selectEntityReference()
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( Book.NAME, Author.NAME ),
					b -> {},
					StubSearchWorkBehavior.of(
							2L,
							reference( Book.NAME, "1" ),
							reference( Author.NAME, "2" )
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					PojoEntityReference.withName( Book.class, Book.NAME, 1 ),
					PojoEntityReference.withName( Author.class, Author.NAME, 2 )
			);
		}
	}

	@Test
	void target_byClass_invalidClass_noEntitySubType() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			Class<?> invalidClass = String.class;

			assertThatThrownBy( () -> searchSession.scope( invalidClass ) )
					.hasMessageContainingAll( "No matching indexed entity types for classes [" + invalidClass.getName() + "]",
							"Neither these classes nor any of their subclasses are indexed in Hibernate Search",
							"Valid classes are: ["
									+ Object.class.getName() + ", "
									+ Book.class.getName() + ", "
									+ Author.class.getName()
									+ "]" );
		}
	}

	@Test
	void target_byClass_invalidClass_noIndexedSubtype() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			Class<?> invalidClass = NotIndexed.class;

			assertThatThrownBy( () -> searchSession.scope( invalidClass ) )
					.hasMessageContainingAll( "No matching indexed entity types for classes [" + invalidClass.getName() + "]",
							"Neither these classes nor any of their subclasses are indexed in Hibernate Search",
							"Valid classes are: ["
									+ Object.class.getName() + ", "
									+ Book.class.getName() + ", "
									+ Author.class.getName()
									+ "]" );
		}
	}

	@Test
	void target_byName_singleType() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchQuery<EntityReference> query = searchSession.search( searchSession.scope( Book.class, Book.NAME ) )
					.selectEntityReference()
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( Book.NAME ),
					b -> {},
					StubSearchWorkBehavior.of(
							3L,
							reference( Book.NAME, "1" ),
							reference( Book.NAME, "2" ),
							reference( Book.NAME, "3" )
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					PojoEntityReference.withName( Book.class, Book.NAME, 1 ),
					PojoEntityReference.withName( Book.class, Book.NAME, 2 ),
					PojoEntityReference.withName( Book.class, Book.NAME, 3 )
			);
		}
	}

	@Test
	void target_byName_multipleTypes() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchQuery<EntityReference> query = searchSession.search( searchSession.scope(
					Object.class, Arrays.asList( Book.NAME, Author.NAME )
			) )
					.selectEntityReference()
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( Book.NAME, Author.NAME ),
					b -> {},
					StubSearchWorkBehavior.of(
							2L,
							reference( Book.NAME, "1" ),
							reference( Author.NAME, "2" )
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					PojoEntityReference.withName( Book.class, Book.NAME, 1 ),
					PojoEntityReference.withName( Author.class, Author.NAME, 2 )
			);
		}
	}

	@Test
	void target_byName_invalidType() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			Class<?> invalidClass = String.class;

			assertThatThrownBy( () -> searchSession.scope(
					invalidClass, Book.NAME
			) )
					.hasMessageContainingAll(
							"Invalid type for '" + Book.NAME + "'",
							"the entity type must extend '" + invalidClass.getName()
									+ "', but entity type '" + Book.class.getName() + "' does not"
					);
		}
	}

	@Test
	void target_byName_invalidName() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			String invalidName = "foo";

			assertThatThrownBy( () -> searchSession.scope(
					Book.class, invalidName
			) )
					.hasMessageContainingAll(
							"No matching indexed entity types for entity names [" + invalidName + "]",
							"Either these are not the names of entity types",
							"or neither the entity types nor any of their subclasses are indexed in Hibernate Search",
							"Valid entity names are: ["
									+ Book.NAME + ", "
									+ Author.NAME
									// NotIndexed should not be mentioned here
									+ "]"
					);
		}
	}

	@SearchEntity(name = Book.NAME)
	@Indexed
	public static class Book {

		public static final String NAME = "Book";

		@DocumentId
		private Integer id;

		@GenericField
		private String title;

		@IndexedEmbedded
		private Author author;

		public Book() {
		}

		public Book(Integer id, String title) {
			this.id = id;
			this.title = title;
		}

		public Integer getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public Author getAuthor() {
			return author;
		}

		public void setAuthor(Author author) {
			this.author = author;
		}
	}

	@SearchEntity(name = Author.NAME)
	@Indexed
	public static class Author {

		public static final String NAME = "Author";

		@DocumentId
		private Integer id;

		@GenericField
		private String name;

		@AssociationInverseSide(inversePath = @ObjectPath(value = @PropertyValue(propertyName = "author")))
		private final List<Book> books = new ArrayList<>();

		public Author() {
		}

		public Author(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<Book> getBooks() {
			return books;
		}
	}

	@SearchEntity(name = NotIndexed.NAME)
	public static class NotIndexed {

		public static final String NAME = "NotInd";

		private Integer id;

		private String name;

		public NotIndexed() {
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	private static class Book_Author {

		private Book book;

		private String author;

		public Book_Author(Book book, String author) {
			this.book = book;
			this.author = author;
		}

		@Override
		public boolean equals(Object obj) {
			if ( !( obj instanceof Book_Author ) ) {
				return false;
			}
			Book_Author other = (Book_Author) obj;
			return Objects.equals( book, other.book )
					&& Objects.equals( author, other.author );
		}

		@Override
		public int hashCode() {
			return Objects.hash( book, author );
		}

		@Override
		public String toString() {
			return book + " - " + author;
		}
	}

	private static class Book_Author_Score {

		private Book_Author book_author;

		private Float score;

		public Book_Author_Score(Book_Author book_author, Float score) {
			this.book_author = book_author;
			this.score = score;
		}

		@Override
		public boolean equals(Object obj) {
			if ( !( obj instanceof Book_Author_Score ) ) {
				return false;
			}
			Book_Author_Score other = (Book_Author_Score) obj;
			return Objects.equals( book_author, other.book_author )
					&& Objects.equals( score, other.score );
		}

		@Override
		public int hashCode() {
			return Objects.hash( book_author, score );
		}

		@Override
		public String toString() {
			return book_author + " - " + score;
		}
	}
}
