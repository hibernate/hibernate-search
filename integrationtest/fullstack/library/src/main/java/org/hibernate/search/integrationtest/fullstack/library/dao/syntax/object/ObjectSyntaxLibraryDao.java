/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.library.dao.syntax.object;

import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.search.mapper.orm.jpa.FullTextQuery;
import org.hibernate.search.mapper.orm.jpa.FullTextSearchTarget;
import org.hibernate.search.integrationtest.fullstack.library.dao.LibraryDao;
import org.hibernate.search.integrationtest.fullstack.library.model.Library;

class ObjectSyntaxLibraryDao extends LibraryDao {

	ObjectSyntaxLibraryDao(EntityManager entityManager) {
		super( entityManager );
	}

	@Override
	public List<Library> search(String terms, int offset, int limit) {
		if ( terms == null || terms.isEmpty() ) {
			return Collections.emptyList();
		}

		FullTextSearchTarget<Library> target = entityManager.search( Library.class );

		FullTextQuery<Library> query = target.query()
				.asEntity()
				.predicate(
						target.predicate().match().onField( "name" ).matching( terms ).toPredicate()
				)
				.sort(
						target.sort()
						.byField( "collectionSize" ).desc()
						.then().byField( "name_sort" )
						.toSort()
				)
				.build();

		query.setFirstResult( offset );
		query.setMaxResults( limit );

		return query.getResultList();
	}

}
