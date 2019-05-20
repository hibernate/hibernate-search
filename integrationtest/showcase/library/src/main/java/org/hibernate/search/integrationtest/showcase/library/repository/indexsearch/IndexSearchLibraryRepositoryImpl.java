/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.repository.indexsearch;

import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.search.integrationtest.showcase.library.model.Library;
import org.hibernate.search.mapper.orm.Search;

import org.springframework.beans.factory.annotation.Autowired;

public class IndexSearchLibraryRepositoryImpl implements IndexSearchLibraryRepository {

	@Autowired
	private EntityManager entityManager;

	@Override
	public List<Library> search(String terms, int limit, int offset) {
		if ( terms == null || terms.isEmpty() ) {
			return Collections.emptyList();
		}
		return Search.getSearchSession( entityManager )
				.search( Library.class )
				.asEntity()
				.predicate( f -> f.match().onField( "name" ).matching( terms ) )
				.sort( c -> {
					c.byField( "collectionSize" ).desc();
					c.byField( "name_sort" );
				} )
				.fetchHits( limit, offset );
	}
}
