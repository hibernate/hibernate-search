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

import org.hibernate.search.integrationtest.showcase.library.model.Person;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.engine.search.query.SearchQuery;

import org.springframework.beans.factory.annotation.Autowired;

public class IndexSearchPersonRepositoryImpl implements IndexSearchPersonRepository {

	@Autowired
	private EntityManager entityManager;

	@Override
	public List<Person> listTopBorrowers(long limit, long offset) {
		return listTopBorrowers( "account.borrowals.totalCount", limit, offset );
	}

	@Override
	public List<Person> listTopShortTermBorrowers(long limit, long offset) {
		return listTopBorrowers( "account.borrowals.shortTermCount", limit, offset );
	}

	@Override
	public List<Person> listTopLongTermBorrowers(long limit, long offset) {
		return listTopBorrowers( "account.borrowals.longTermCount", limit, offset );
	}

	@Override
	public List<Person> searchPerson(String terms, long limit, long offset) {
		if ( terms == null || terms.isEmpty() ) {
			return Collections.emptyList();
		}

		SearchQuery<Person> query = Search.getSearchSession( entityManager ).search( Person.class )
				.asEntity()
				.predicate( f -> f.match().onFields( "firstName", "lastName" ).matching( terms ) )
				.sort( c -> {
					c.byField( "lastName_sort" );
					c.byField( "firstName_sort" );
				} )
				.toQuery();

		return query.fetchHits( limit, offset );
	}

	private List<Person> listTopBorrowers(String borrowalsCountField, long limit, long offset) {
		SearchQuery<Person> query = Search.getSearchSession( entityManager ).search( Person.class )
				.asEntity()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( borrowalsCountField ).desc() )
				.toQuery();

		return query.fetchHits( limit, offset );
	}

}
