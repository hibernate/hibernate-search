/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.factory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.genericjpa.annotations.DtoField;
import org.hibernate.search.genericjpa.annotations.DtoOverEntity;
import org.hibernate.search.genericjpa.factory.StandaloneSearchConfiguration;
import org.hibernate.search.genericjpa.factory.StandaloneSearchFactory;
import org.hibernate.search.genericjpa.factory.StandaloneSearchFactoryFactory;
import org.hibernate.search.genericjpa.factory.Transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class IndexOperationsTest {

	private static final Logger LOGGER = Logger.getLogger( IndexOperationsTest.class.getName() );
	private StandaloneSearchFactory factory;

	@Before
	public void setup() {
		LOGGER.info( "setting up IndexOperationsTest" );
		this.factory = StandaloneSearchFactoryFactory.createSearchFactory(
				new StandaloneSearchConfiguration(), Arrays.asList(
						Book.class
				)
		);
	}


	@Test
	public void testRollback() {
		Book book = new Book( 1, "TestBook" );
		Transaction tx = new Transaction();
		this.factory.index( book, tx );
		tx.rollback();
		this.assertCount( 0 );
	}

	@Test
	public void testRollbackFlush() {
		Book book = new Book( 1, "TestBook" );
		Transaction tx = new Transaction();
		this.factory.index( book, tx );
		this.factory.flushToIndexes( tx );
		tx.rollback();
		this.assertCount( 1 );
	}

	@Test
	public void test() {
		List<Book> l = new ArrayList<>();
		l.add( new Book( 1, "The Hobbit" ) );
		l.add( new Book( 2, "Lord of the Rings" ) );

		this.factory.index( l );
		this.assertCount( 2 );

		this.factory.purgeAll( Book.class );
		this.assertCount( 0 );

		this.factory.index( l.get( 0 ) );
		this.assertCount( 1 );
		this.factory.index( l.get( 1 ) );
		this.assertCount( 2 );

		{
			Transaction tc = new Transaction();
			this.factory.purgeAll( Book.class, tc );
			tc.commit();
		}
		this.assertCount( 0 );

		{
			Transaction tc = new Transaction();
			this.factory.index( l, tc );
			tc.commit();
		}
		this.assertCount( 2 );

		this.factory.purgeAll( Book.class );
		this.assertCount( 0 );

		{
			Transaction tc = new Transaction();
			this.factory.index( l.get( 0 ), tc );
			tc.commit();
		}
		this.assertCount( 1 );
		this.factory.purgeAll( Book.class );
		this.assertCount( 0 );

		this.factory.index( l );
		this.assertCount( 2 );

		this.factory.delete( l );
		this.assertCount( 0 );

		this.factory.index( l );
		this.assertCount( 2 );

		this.factory.delete( l.get( 0 ) );
		this.assertCount( 1 );
		this.factory.delete( l.get( 1 ) );
		this.assertCount( 0 );

		this.factory.index( l );
		this.assertCount( 2 );

		{
			Transaction tc = new Transaction();
			this.factory.delete( l, tc );
			tc.commit();
		}
		this.assertCount( 0 );

		this.factory.index( l );
		this.assertCount( 2 );

		{
			Transaction tc = new Transaction();
			this.factory.delete( l.get( 0 ), tc );
			tc.commit();
		}
		this.assertCount( 1 );
		this.factory.purgeAll( Book.class );
		this.assertCount( 0 );

		this.factory.index( l );
		this.assertCount( 2 );

		List<Book> updated = new ArrayList<>();
		updated.add( new Book( 1, "The ultimate Hobbit" ) );
		updated.add( new Book( 2, "Lord of The ultimate Rings" ) );
		this.factory.update( updated );
		this.assertCount( 2 );
		assertNotEquals( this.all(), l );

		this.factory.purgeAll( Book.class );
		this.factory.index( l );
		this.assertCount( 2 );

		int id = updated.get( 0 ).getId();
		this.factory.update( updated.get( 0 ) );
		this.assertCount( 2 );
		assertNotEquals( this.id( id ), l.get( 0 ) );

		this.factory.purgeAll( Book.class );
		this.factory.index( l );
		this.assertCount( 2 );

		{
			Transaction tc = new Transaction();
			id = updated.get( 0 ).getId();
			this.factory.update( updated.get( 0 ), tc );
			tc.commit();
		}
		this.assertCount( 2 );
		assertNotEquals( this.id( id ), l.get( 0 ) );

		{
			Transaction tc = new Transaction();
			this.factory.update( updated, tc );
			tc.commit();
		}
		this.assertCount( 2 );
		assertNotEquals( this.all(), l );

		this.factory.purge( Book.class, new TermQuery( new Term( "id", "1" ) ) );
		this.assertCount( 1 );

		{
			Transaction tc = new Transaction();
			this.factory.purge( Book.class, new TermQuery( new Term( "id", "2" ) ), tc );
			tc.commit();
		}
		this.assertCount( 0 );
	}

	@Test
	public void testPurgeByTerm() {
		//test purgeByTerm for a String
		{
			this.factory.index( new Book( 1, "The Hitchhiker’s Guide to the Galaxy" ) );
			this.assertCount( 1 );
			//DocumentId is a string
			this.factory.purgeByTerm( Book.class, "id", "1" );
			this.assertCount( 0 );
		}

		//test purgeByTerm for a String with a Transaction
		{
			this.factory.index( new Book( 1, "Hitchhiker again" ) );
			Transaction tx = new Transaction();
			this.factory.purgeByTerm( Book.class, "id", "1", tx );
			this.assertCount( 1 );
			tx.commit();
			this.assertCount( 0 );
		}

		//test for something different than a string
		{
			{
				this.factory.index( new Book( new Integer( 2 ) ) );
				this.assertCount( 1 );
				this.factory.purgeByTerm( Book.class, "someInt", 2 );
				this.assertCount( 0 );
			}

			{
				this.factory.index( new Book( new Long( 3L ) ) );
				this.assertCount( 1 );
				this.factory.purgeByTerm( Book.class, "someLong", 3L );
				this.assertCount( 0 );
			}

			{
				this.factory.index( new Book( new Float( 4.5F ) ) );
				this.assertCount( 1 );
				this.factory.purgeByTerm( Book.class, "someFloat", 4.5F );
				this.assertCount( 0 );
			}

			{
				this.factory.index( new Book( new Double( 5.5D ) ) );
				this.assertCount( 1 );
				this.factory.purgeByTerm( Book.class, "someDouble", 5.5D );
				this.assertCount( 0 );
			}
		}

		//the same thing, but with a Transaction
		{
			{
				Transaction tx = new Transaction();
				this.factory.index( new Book( new Integer( 2 ) ) );
				this.assertCount( 1 );
				this.factory.purgeByTerm( Book.class, "someInt", 2, tx );
				tx.commit();
				this.assertCount( 0 );
			}

			{
				Transaction tx = new Transaction();
				this.factory.index( new Book( new Long( 3L ) ) );
				this.assertCount( 1 );
				this.factory.purgeByTerm( Book.class, "someLong", 3L, tx );
				tx.commit();
				this.assertCount( 0 );
			}

			{
				Transaction tx = new Transaction();
				this.factory.index( new Book( new Float( 4.5F ) ) );
				this.assertCount( 1 );
				this.factory.purgeByTerm( Book.class, "someFloat", 4.5F, tx );
				tx.commit();
				this.assertCount( 0 );
			}

			{
				Transaction tx = new Transaction();
				this.factory.index( new Book( new Double( 5.5D ) ) );
				this.assertCount( 1 );
				this.factory.purgeByTerm( Book.class, "someDouble", 5.5D, tx );
				tx.commit();
				this.assertCount( 0 );
			}
		}
	}

	@Test
	public void testHSQuery() {
		this.factory.index( new Book( 1, "The Hitchhiker’s Guide to the Galaxy" ) );
		this.assertCount( 1 );

		{
			Book book = this.factory.createQuery(
					this.factory.buildQueryBuilder()
							.forEntity( Book.class )
							.get()
							.keyword()
							.onField( "id" )
							.matching( 1 )
							.createQuery(), Book.class
			).queryDto( Book.class, "otherProfile" ).get( 0 );
			assertNull( book.getId() );
			assertNull( book.getTitle() );
			assertEquals( "The Hitchhiker’s Guide to the Galaxy", book.getTitle2() );
		}

		{
			Book book = this.factory.createQuery(
					this.factory.buildQueryBuilder()
							.forEntity( Book.class )
							.get()
							.keyword()
							.onField( "id" )
							.matching( 1 )
							.createQuery(), Book.class
			).queryDto( Book.class ).get( 0 );
			assertEquals( "The Hitchhiker’s Guide to the Galaxy", book.getTitle() );
			assertEquals( new Integer( 1 ), book.getId() );
			assertNull( book.getTitle2() );
		}
	}

	private void assertCount(int count) {
		assertEquals(
				count, this.factory.createQuery(
						this.factory.buildQueryBuilder()
								.forEntity( Book.class )
								.get()
								.all()
								.createQuery(), Book.class
				)
						.queryResultSize()
		);
	}

	private List<Book> all() {
		return this.factory.createQuery(
				this.factory.buildQueryBuilder()
						.forEntity( Book.class )
						.get()
						.all()
						.createQuery(), Book.class
		).maxResults( 10 )
				.queryDto( Book.class );
	}

	private Book id(int id) {
		return this.factory
				.createQuery(
						this.factory.buildQueryBuilder()
								.forEntity( Book.class )
								.get()
								.keyword()
								.onField( "id" )
								.matching( String.valueOf( id ) )
								.createQuery(),
						Book.class
				).maxResults( 10 ).queryDto( Book.class ).get( 0 );
	}

	@After
	public void tearDown() throws IOException {
		LOGGER.info( "tearing down IndexOperationsTest" );
		this.factory.close();
	}

	@Indexed
	@DtoOverEntity(entityClass = Book.class)
	public static final class Book {

		@DtoField(fieldName = "id")
		private Integer id;
		@DtoField(fieldName = "title")
		private String title;
		@DtoField(fieldName = "title", profileName = "otherProfile")
		private String title2;

		@Field
		private Integer someInt;
		@Field
		private Long someLong;
		@Field
		private Float someFloat;
		@Field
		private Double someDouble;

		public Book() {

		}

		public Book(int id, String title) {
			this.id = id;
			this.title = title;
		}

		public Book(Integer someInt) {
			this( 1, "someTitle" );
			this.someInt = someInt;
		}

		public Book(Long someLong) {
			this( 1, "someTitle" );
			this.someLong = someLong;
		}

		public Book(Float someFloat) {
			this( 1, "someTitle" );
			this.someFloat = someFloat;
		}

		public Book(Double someDouble) {
			this( 1, "someTitle" );
			this.someDouble = someDouble;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getTitle2() {
			return title2;
		}

		public void setTitle2(String title2) {
			this.title2 = title2;
		}

		@DocumentId
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Field(store = Store.YES, index = Index.YES)
		public String getTitle() {
			return this.title;
		}

		public void setText(String title) {
			this.title = title;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
			result = prime * result + ((title == null) ? 0 : title.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			Book other = (Book) obj;
			if ( id != other.id ) {
				return false;
			}
			if ( title == null ) {
				if ( other.title != null ) {
					return false;
				}
			}
			else if ( !title.equals( other.title ) ) {
				return false;
			}
			return true;
		}

	}

}
