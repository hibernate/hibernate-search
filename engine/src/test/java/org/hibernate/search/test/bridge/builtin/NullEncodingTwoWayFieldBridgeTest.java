/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge.builtin;

import java.util.Date;

import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.builtin.NumericEncodingDateBridge;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Rule;
import org.junit.Test;

/**
 * Verifies the "null marker" is stored in the index when we wrap another
 * {@link FieldBridge} with a {@link org.hibernate.search.bridge.builtin.impl.NullEncodingTwoWayFieldBridge}
 *
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "HSEARCH-1825")
public class NullEncodingTwoWayFieldBridgeTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Sample.class )
		.withProperty( org.hibernate.search.cfg.Environment.DEFAULT_NULL_TOKEN, "-1" );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void testIndexingWithNullEncodingFieldBridge() {
		Sample entity = new Sample();
		entity.id = 1;
		entity.description = "null date";
		entity.deletionDate = null; // should trigger the marker token
		helper.add( entity );

		Query termQuery = NumericFieldUtils.createExactMatchQuery( "deletionDate", Long.parseLong( "-1" ) );
		helper.assertThat( termQuery )
				.from( Sample.class )
				.hasResultSize( 1 );
	}

	@Indexed
	static class Sample {

		@DocumentId
		long id;

		@Field
		String description;

		@Field(store = Store.YES, index = Index.YES, analyze = Analyze.NO, indexNullAs = Field.DEFAULT_NULL_TOKEN,
				bridge = @FieldBridge(impl = NumericEncodingDateBridge.class, params =
					{ @org.hibernate.search.annotations.Parameter(name = "resolution", value = "MINUTE") }))
		private Date deletionDate;
	}

}
