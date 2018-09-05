/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jpa;

import org.hibernate.search.orm.spi.SearchIntegratorHelper;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-2901")
public class IntegratorExtractionTest extends JPATestCase {

	@Test
	public void extractFromEMF() {
		SearchIntegrator si = SearchIntegratorHelper.extractFromEntityManagerFactory( factory );
		Assert.assertNotNull( si );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Bretzel.class };
	}

}
