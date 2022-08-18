/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.standalone.common.EntityReference;
import org.hibernate.search.mapper.pojo.standalone.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test everything related to the search query itself.
 * <p>
 * Does not test sorts and predicates, or other features that only involve the backend.
 * Those should be tested in the backend integration tests.
 */
public class SearchQueryBaseIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@Before
	public void setup() {
		backendMock.expectAnySchema( Book.NAME );
		backendMock.expectAnySchema( Author.NAME );

		mapping = setupHelper.start()
				.withAnnotatedEntityType( Book.class, Book.NAME )
				.withAnnotatedEntityType( Author.class, Author.NAME )
				.withAnnotatedEntityType( NotIndexed.class, NotIndexed.NAME )
				.setup();
	}

	@Test
	public void target_byClass_singleType() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchQuery<EntityReference> query = searchSession.search( Book.class )
					.selectEntityReference()
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( Book.NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							3L,
							reference( Book.NAME, "1" ),
							reference( Book.NAME, "2" ),
							reference( Book.NAME, "3" )
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					EntityReferenceImpl.withName( Book.class, Book.NAME, 1 ),
					EntityReferenceImpl.withName( Book.class, Book.NAME, 2 ),
					EntityReferenceImpl.withName( Book.class, Book.NAME, 3 )
			);
		}
	}

	@Test
	public void target_byClass_multipleTypes() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			SearchQuery<EntityReference> query = searchSession.search( Arrays.asList( Book.class, Author.class ) )
					.selectEntityReference()
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( Book.NAME, Author.NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							2L,
							reference( Book.NAME, "1" ),
							reference( Author.NAME, "2" )
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					EntityReferenceImpl.withName( Book.class, Book.NAME, 1 ),
					EntityReferenceImpl.withName( Author.class, Author.NAME, 2 )
			);
		}
	}

	@Test
	public void target_byClass_invalidClass() {
		try ( SearchSession searchSession = mapping.createSession() ) {
			Class<?> invalidClass = String.class;

			assertThatThrownBy( () -> searchSession.scope( invalidClass ) )
					.hasMessageContainingAll( "No matching indexed entity types for types: [" + invalidClass.getName() + "]",
							"These types are not indexed entity types, nor is any of their subtypes",
							"Valid indexed entity classes, superclasses and superinterfaces are: ["
									+ Object.class.getName() + ", "
									+ Book.class.getName() + ", "
									+ Author.class.getName()
									+ "]" );
		}
	}

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
			if ( !(obj instanceof Book_Author) ) {
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
			if ( !(obj instanceof Book_Author_Score) ) {
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
