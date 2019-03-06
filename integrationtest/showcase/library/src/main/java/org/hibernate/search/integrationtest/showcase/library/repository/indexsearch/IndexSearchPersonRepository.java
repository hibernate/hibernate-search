/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.repository.indexsearch;

import java.util.List;

import org.hibernate.search.integrationtest.showcase.library.model.Person;

public interface IndexSearchPersonRepository {

	List<Person> listTopBorrowers(long offset, long limit);

	List<Person> listTopShortTermBorrowers(long offset, long limit);

	List<Person> listTopLongTermBorrowers(long offset, long limit);

	List<Person> searchPerson(String terms, long offset, long limit);
}
