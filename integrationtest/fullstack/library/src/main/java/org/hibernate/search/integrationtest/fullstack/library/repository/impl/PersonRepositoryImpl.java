/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.library.repository.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.mapper.orm.jpa.FullTextQuery;
import org.hibernate.search.mapper.orm.jpa.FullTextSearchTarget;
import org.hibernate.search.integrationtest.fullstack.library.repository.PersonRepository;
import org.hibernate.search.integrationtest.fullstack.library.model.Person;

class PersonRepositoryImpl extends PersonRepository {

	@Override
	public List<Person> search(String terms, int offset, int limit) {
		if ( terms == null || terms.isEmpty() ) {
			return Collections.emptyList();
		}

		FullTextSearchTarget<Person> target = entityManager.search( Person.class );

		FullTextQuery<Person> query = target.query()
				.asEntity()
				.predicate( f -> f.match().onFields( "firstName", "lastName" ).matching( terms ).toPredicate() )
				.sort( c -> {
					c.byField( "lastName_sort" );
					c.byField( "firstName_sort" );
				} )
				.build();

		query.setFirstResult( offset );
		query.setMaxResults( limit );

		return query.getResultList();
	}

	@Override
	public List<Person> listTopBorrowers(String borrowalsCountField, int offset, int limit) {
		FullTextSearchTarget<Person> target = entityManager.search( Person.class );

		FullTextQuery<Person> query = target.query()
				.asEntity()
				.predicate( f -> f.matchAll().toPredicate() )
				.sort( c -> c.byField( borrowalsCountField ).desc() )
				.build();

		query.setFirstResult( offset );
		query.setMaxResults( limit );

		return query.getResultList();
	}
}
