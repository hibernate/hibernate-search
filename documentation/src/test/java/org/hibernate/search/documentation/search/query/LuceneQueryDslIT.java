/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.search.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;

public class LuceneQueryDslIT {

	private static final int BOOK1_ID = 1;
	private static final int BOOK2_ID = 2;
	private static final int BOOK3_ID = 3;
	private static final int BOOK4_ID = 4;

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.setup( Book.class );
		initData();
	}

	@Test
	public void explain() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::explain-lucene[]
			LuceneSearchQuery<Book> query = searchSession.search( Book.class )
					.extension( LuceneExtension.get() ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.toQuery(); // <2>

			Explanation explanation1 = query.explain( 1 ); // <3>
			Explanation explanation2 = query.explain( "Book", 1 ); // <4>

			LuceneSearchQuery<Book> luceneQuery = query.extension( LuceneExtension.get() ); // <5>
			// end::explain-lucene[]

			assertThat( explanation1 ).asString()
					.contains( "title" );
			assertThat( explanation2 ).asString()
					.contains( "title" );
			assertThat( luceneQuery ).isNotNull();
		} );
	}

	@Test
	public void lowLevel() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			// tag::lucene-lowLevel[]
			LuceneSearchQuery<Book> query = searchSession.search( Book.class )
					.extension( LuceneExtension.get() ) // <1>
					.where( f -> f.match()
							.field( "title" )
							.matching( "robot" ) )
					.sort( f -> f.field( "title_sort" ) )
					.toQuery(); // <2>

			Sort sort = query.luceneSort(); // <3>

			LuceneSearchResult<Book> result = query.fetch( 20 ); // <4>

			TopDocs topDocs = result.topDocs(); // <5>
			// end::lucene-lowLevel[]

			assertThat( result.hits() ).extracting( Book::getId )
					.containsExactly( BOOK1_ID, BOOK3_ID );

			assertThat( sort ).isNotNull();
			assertThat( sort.getSort() ).hasSize( 1 );
			assertThat( sort.getSort()[0].getType() ).isEqualTo( SortField.Type.CUSTOM );

			assertThat( topDocs ).isNotNull();
			assertThat( topDocs.totalHits.value ).isEqualTo( 2L );
			assertThat( topDocs.scoreDocs ).hasSize( 2 );
		} );
	}

	private void initData() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book1 = new Book();
			book1.setId( BOOK1_ID );
			book1.setTitle( "I, Robot" );

			Book book2 = new Book();
			book2.setId( BOOK2_ID );
			book2.setTitle( "The Caves of Steel" );

			Book book3 = new Book();
			book3.setId( BOOK3_ID );
			book3.setTitle( "The Robots of Dawn" );

			Book book4 = new Book();
			book4.setId( BOOK4_ID );
			book4.setTitle( "The Automatic Detective" );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
			entityManager.persist( book3 );
			entityManager.persist( book4 );
		} );
	}


}
