/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.step.lucene.impl;

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
	 * Index all the entities found.
	 */
	FULL_ENTITY
}
