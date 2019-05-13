/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class DuplicateFieldTest {

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private SearchIntegrator searchIntegrator;

	@After
	public void cleanup() {
		if ( searchIntegrator != null ) {
			searchIntegrator.close();
		}
	}

	@Test
	public void shouldWarnOnTwoFieldDefinitionsWithSameNameButDifferentIndexSetting() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( SampleWithTwoFieldDefinitionsWithSameNameButDifferentIndexSetting.class );

		logged.expectMessage(
				"HSEARCH000120",
				"There are multiple properties indexed against the same field name '"
						+ SampleWithTwoFieldDefinitionsWithSameNameButDifferentIndexSetting.class.getName() + ".theField'",
				"with different indexing settings",
				"The behaviour is undefined"
		);

		this.searchIntegrator = new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator();
	}

	@Indexed
	static class SampleWithTwoFieldDefinitionsWithSameNameButDifferentIndexSetting {

		@DocumentId
		long id;

		@Field(name = "theField")
		String someProperty;

		@Field(name = "theField", index = Index.NO)
		String otherProperty;
	}
}
