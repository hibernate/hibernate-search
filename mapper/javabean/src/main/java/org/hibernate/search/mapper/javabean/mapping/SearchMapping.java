/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.javabean.session.SearchSessionBuilder;

public interface SearchMapping {

	/**
	 * @return A new session allowing to {@link SearchSession#getMainWorkPlan() index} or
	 * {@link SearchSession#search(Class) search for} entities.
	 * @see #createSessionWithOptions()
	 */
	SearchSession createSession();

	/**
	 * @return A session builder allowing to more finely configure the new session.
	 * @see #createSession()
	 */
	SearchSessionBuilder createSessionWithOptions();

	static SearchMappingBuilder builder() {
		return builder( MethodHandles.publicLookup() );
	}

	static SearchMappingBuilder builder(MethodHandles.Lookup lookup) {
		return new SearchMappingBuilder( lookup );
	}

}
