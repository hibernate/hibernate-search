/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

abstract class Call<S extends Call<S>> {

	/**
	 * @param other Another call of similar type.
	 * @return {@code true} if the other call is similar, i.e. a first cursory check indicates it could be identical.
	 * {@code false} if there is no chance it could be identical.
	 */
	abstract boolean isSimilarTo(S other);

}
