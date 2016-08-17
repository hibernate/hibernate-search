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
public class JobSEEnvironment {

	private static EntityManagerFactory emf;

	public static EntityManagerFactory getEntityManagerFactory() {
		return emf;
	}

	public static void setEntityManagerFactory( EntityManagerFactory entityManagerFactory ) {
		emf = entityManagerFactory;
	}
}
