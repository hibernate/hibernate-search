/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.dsl;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Rule;
import org.junit.Test;

/**
 * The RangeQuery builder DSL needs to guess the right type of range it needs to produce, by either relying
 * on the known metadata for the target fields, or by guessing it from the types provided as arguments.
 *
 * @author Sanne Grinovero (C) 2014 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1758")
public class NumericTypeGuessedTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( CustomBridgedNumbers.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void numericRangeQueryOnCustomField() {
		storeData( "title-one", "1" );
		storeData( "title-two", "2" );
		storeData( "title-three", "3" );

		QueryBuilder queryBuilder = helper.queryBuilder( CustomBridgedNumbers.class );

		Query query = queryBuilder
					.range()
						.onField( "customField" )
						.from( 1 ).excludeLimit()
						.to( 3 ).excludeLimit()
						.createQuery();

		helper.assertThat( query )
				.from( CustomBridgedNumbers.class )
				.projecting( "title" )
				.matchesExactlySingleProjections( "title-two" );
	}

	private void storeData(String title, String value) {
		CustomBridgedNumbers entry = new CustomBridgedNumbers();
		entry.title = title;
		entry.textEncodedInt = value;
		helper.add( entry );
	}

	@Indexed
	public static class CustomBridgedNumbers {
		@DocumentId
		String title;

		@Field(bridge = @FieldBridge(impl = NumericEncodingCustom.class))
		String textEncodedInt;
	}

	public static class NumericEncodingCustom implements org.hibernate.search.bridge.FieldBridge, MetadataProvidingFieldBridge {

		@Override
		public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
			builder.field( "customField", FieldType.INTEGER );
		}

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			if ( value != null ) {
				Integer i = Integer.parseInt( (String) value );
				luceneOptions.addNumericFieldToDocument( "customField", i, document );
			}
		}

	}

}
