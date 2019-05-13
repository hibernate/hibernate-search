/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.document.Document;

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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3562") // This used to fail because some metadata was accessed before it was created
	public void shouldWarnButNotFailOnTwoFieldDefinitionsWithSameNameButDifferentIndexSettingFromClassBridges() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( SampleWithTwoFieldDefinitionsWithSameNameButDifferentIndexSettingFromClassBridge.class );

		logged.expectMessage(
				"HSEARCH000120",
				"There are multiple properties indexed against the same field name '"
						+ SampleWithTwoFieldDefinitionsWithSameNameButDifferentIndexSettingFromClassBridge.class.getName() + ".theField'",
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

	@Indexed
	@ClassBridge(name = "theField", impl = MyBridge.class)
	@ClassBridge(name = "theField", impl = MyBridge.class, index = Index.NO)
	static class SampleWithTwoFieldDefinitionsWithSameNameButDifferentIndexSettingFromClassBridge {

		@DocumentId
		long id;

		String someProperty;

		String otherProperty;
	}

	public static class MyBridge implements MetadataProvidingFieldBridge {
		public MyBridge() {
		}

		@Override
		public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
			// Nothing to do
		}

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			throw new UnsupportedOperationException( "This should not be called" );
		}
	}
}
