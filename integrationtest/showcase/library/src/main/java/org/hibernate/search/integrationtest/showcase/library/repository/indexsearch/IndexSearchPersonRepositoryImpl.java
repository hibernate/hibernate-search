/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.repository.indexsearch;

import java.util.Collections;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.hibernate.search.integrationtest.showcase.library.model.Person;
import org.hibernate.search.mapper.orm.Search;

import org.springframework.beans.factory.annotation.Autowired;

public class IndexSearchPersonRepositoryImpl implements IndexSearchPersonRepository {

	@Autowired
	private EntityManager entityManager;

	@Override
	public List<Person> listTopBorrowers(int offset, int limit) {
		return listTopBorrowers( "account.borrowals.totalCount", limit, offset );
	}

	@Override
	public List<Person> listTopShortTermBorrowers(int offset, int limit) {
		return listTopBorrowers( "account.borrowals.shortTermCount", limit, offset );
	}

	@Override
	public List<Person> listTopLongTermBorrowers(int offset, int limit) {
		return listTopBorrowers( "account.borrowals.longTermCount", limit, offset );
	}

	@Override
	public List<Person> searchPerson(String terms, int offset, int limit) {
		if ( terms == null || terms.isEmpty() ) {
			return Collections.emptyList();
		}

		return Search.session( entityManager ).search( Person.class )
				.where( f -> f.match().fields( "firstName", "lastName" ).matching( terms ) )
				.sort( f -> f.field( "lastName_sort" )
						.then().field( "firstName_sort" )
				)
				.fetchHits( offset, limit );
	}

	private List<Person> listTopBorrowers(String borrowalsCountField, int limit, int offset) {
		return Search.session( entityManager ).search( Person.class )
				.where( f -> f.matchAll() )
				.sort( f -> f.field( borrowalsCountField ).desc() )
				.fetchHits( offset, limit );
	}

}
