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
import org.hibernate.search.mapper.orm.search.query.FullTextQuery;
import org.hibernate.search.mapper.orm.search.FullTextSearchTarget;

import org.springframework.beans.factory.annotation.Autowired;

public class IndexSearchPersonRepositoryImpl implements IndexSearchPersonRepository {

	@Autowired
	private EntityManager entityManager;

	@Override
	public List<Person> listTopBorrowers(int offset, int limit) {
		return listTopBorrowers( "account.borrowals.totalCount", offset, limit );
	}

	@Override
	public List<Person> listTopShortTermBorrowers(int offset, int limit) {
		return listTopBorrowers( "account.borrowals.shortTermCount", offset, limit );
	}

	@Override
	public List<Person> listTopLongTermBorrowers(int offset, int limit) {
		return listTopBorrowers( "account.borrowals.longTermCount", offset, limit );
	}

	@Override
	public List<Person> searchPerson(String terms, int offset, int limit) {
		if ( terms == null || terms.isEmpty() ) {
			return Collections.emptyList();
		}

		FullTextSearchTarget<Person> target = Search.getFullTextSession( entityManager ).search( Person.class );

		FullTextQuery<Person> query = target.query()
				.asEntity()
				.predicate( f -> f.match().onFields( "firstName", "lastName" ).matching( terms ) )
				.sort( c -> {
					c.byField( "lastName_sort" );
					c.byField( "firstName_sort" );
				} )
				.build();

		query.setFirstResult( offset );
		query.setMaxResults( limit );

		return query.getResultList();
	}

	private List<Person> listTopBorrowers(String borrowalsCountField, int offset, int limit) {
		FullTextSearchTarget<Person> target = Search.getFullTextSession( entityManager ).search( Person.class );

		FullTextQuery<Person> query = target.query()
				.asEntity()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( borrowalsCountField ).desc() )
				.build();

		query.setFirstResult( offset );
		query.setMaxResults( limit );

		return query.getResultList();
	}

}
