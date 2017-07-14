/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import org.hibernate.Criteria;

/**
 * The index scope of a given entity type.
 *
 * @author Mincong Huang
 */
public enum IndexScope {
	/**
	 * Index entities restricted by the HQL / JPQL given by user.
	 */
	HQL,
	/**
	 * Index entities restricted the {@link Criteria} given by user.
	 */
	CRITERIA,
	/**
	 * Index all the entities found.
	 */
	FULL_ENTITY
}
