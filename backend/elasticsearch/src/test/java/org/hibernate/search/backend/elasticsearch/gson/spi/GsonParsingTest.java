/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.gson.spi;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.util.impl.test.JsonHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-4580")
public class GsonParsingTest {

	@Parameterized.Parameters
	public static Object[][] params() {
		return new Object[][] {
				{ "{\"_routing\": {\"required\": true}}", RootTypeMapping.class } // HSEARCH-4580
		};
	}

	private final Gson gson;

	@Parameterized.Parameter(0)
	public String jsonToParse;
	@Parameterized.Parameter(1)
	public Class<?> targetType;

	public GsonParsingTest() {
		gson = GsonProvider.create( GsonBuilder::new, true ).getGsonNoSerializeNulls();
	}

	@Test
	public void parsingWithoutException() {
		assertThatCode( () -> gson.fromJson( jsonToParse, targetType ) ).doesNotThrowAnyException();
	}

	@Test
	public void noInformationLoss() {
		Object parsed = gson.fromJson( jsonToParse, targetType );
		String written = gson.toJson( parsed );
		JsonHelper.assertJsonEquals( jsonToParse, written );
	}
}
