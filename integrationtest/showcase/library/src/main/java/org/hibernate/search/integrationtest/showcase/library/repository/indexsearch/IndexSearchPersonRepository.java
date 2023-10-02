/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.repository.indexsearch;

import java.util.List;

import org.hibernate.search.integrationtest.showcase.library.model.Person;

public interface IndexSearchPersonRepository {

	List<Person> listTopBorrowers(int offset, int limit);

	List<Person> listTopShortTermBorrowers(int offset, int limit);

	List<Person> listTopLongTermBorrowers(int offset, int limit);

	List<Person> searchPerson(String terms, int offset, int limit);
}
