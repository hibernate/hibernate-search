/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.se;

import javax.persistence.EntityManagerFactory;

/**
 * @author Mincong Huang
 */
public final class JobSEEnvironment {

	private static final JobSEEnvironment INSTANCE = new JobSEEnvironment();
	private EntityManagerFactory emf;

	private JobSEEnvironment() {
		if ( INSTANCE != null ) {
			throw new IllegalStateException( "Already instantiated" );
		}
	}

	public static JobSEEnvironment getInstance() {
		return INSTANCE;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return emf;
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		emf = entityManagerFactory;
	}
}
