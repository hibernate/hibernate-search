/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.hibernate.search.integrationtest.fullstack.library.model.Book;
import org.hibernate.search.integrationtest.fullstack.library.repo.DocumentRepo;
import org.hibernate.search.integrationtest.fullstack.support.JtaEnvironment;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.jpa.FullTextEntityManager;
import org.hibernate.search.mapper.orm.jpa.FullTextQuery;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.junit4.WeldInitiator;

public class OrmEsManualIndexingIT {

	private static final int NUMBER_OF_BOOKS = 200;

	@ClassRule
	public static JtaEnvironment jtaEnvironment = new JtaEnvironment();

	@Rule
	public WeldInitiator weld = WeldInitiator.from( new Weld() )
			.activate( RequestScoped.class )
			.inject( this )
			.build();

	@Inject
	private DocumentRepo documentRepo;

	@Before
	public void initData() {
		Book[] books = new Book[NUMBER_OF_BOOKS];
		for ( int i = 0; i < NUMBER_OF_BOOKS; i++ ) {
			String isbn = String.format( Locale.ROOT, "973-0-00-%06d-3", i );

			books[i] = new Book( i, isbn , "Divine Comedy chapter n. " + ( i + 1 ), "Dante Alighieri",
					"The Divine Comedy is composed of 14,233 lines that are divided into three cantiche (singular cantica) – Inferno (Hell), Purgatorio (Purgatory), and Paradiso (Paradise)",
					"literature,poem,afterlife" );
		}
		documentRepo.createBooks( Arrays.asList( books ) );
	}

	@Test
	public void testMassIndexing() {
		checkNothingIsIndexed();
		FullTextEntityManager ftEntityManager = Search.getFullTextEntityManager( documentRepo.entityManager() );
		MassIndexer indexer = ftEntityManager.createIndexer();
		try {
			indexer.startAndWait();
		}
		catch (InterruptedException e) {
			fail( "Unexpected InterruptedException: " + e.getMessage() );
		}
	}

	private void checkNothingIsIndexed() {
		FullTextEntityManager ftEntityManager = Search.getFullTextEntityManager( documentRepo.entityManager() );
		FullTextQuery<Book> query = ftEntityManager.search( Book.class ).query().asEntity()
				.predicate( context -> context.matchAll().toPredicate() ).build();
		List<Book> books = query.getResultList();

		assertThat( books ).hasSize( 0 );
	}

}
