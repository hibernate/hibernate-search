/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.util;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Locale;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.mapper.orm.Search;

public final class BookCreatorUtils {
	private BookCreatorUtils() {
	}

	public static void prepareBooks(EntityManagerFactory entityManagerFactory, int numberOfBooks) {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			for ( int i = 0; i < numberOfBooks; i++ ) {
				Book book = new Book();
				book.setId( i + 1 );
				book.setTitle( String.format( Locale.ROOT, "Very interesting book title. Edition # %d", i + 1 ) );
				entityManager.persist( book );
			}
		} );
	}

	public static Long documentsCount(EntityManagerFactory entityManagerFactory) {
		return with( entityManagerFactory ).applyInTransaction(
				session -> Search.session( session )
						.search( Book.class )
						.where( f -> f.matchAll() )
						.fetchTotalHitCount()
		);
	}
}
