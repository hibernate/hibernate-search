/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.search.query;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ElasticsearchBoolSearchPredicateIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	public void resultingQueryOptimization() {
		SearchPredicateFactory f = index.createScope().predicate();
		BooleanPredicateClausesStep<?> step = f.bool().must( f.not( f.not(
				f.not( f.not( f.not( f.not( f.not( f.match().field( "fieldName" ).matching( "test" ) ) ) ) ) ) ) ) );

		assertThat(
				new Gson().fromJson(
						index.query()
								.where( ( step ).toPredicate() )
								.toQuery()
								.queryString(),
						JsonObject.class
				)
		).isEqualTo( new Gson().fromJson(
				"{" +
						"  \"query\": {" +
						"    \"bool\": {" +
						"      \"must_not\": {" +
						"        \"match\": {" +
						"          \"fieldName\": {" +
						"            \"query\": \"test\"" +
						"          }" +
						"        }" +
						"      }" +
						"    }" +
						"  }," +
						"  \"_source\": false" +
						"}",
				JsonObject.class
		) );

		assertThat(
				new Gson().fromJson(
						index.query()
								.where( f.not( ( step ) ).toPredicate() )
								.toQuery()
								.queryString(),
						JsonObject.class
				)
		).isEqualTo( new Gson().fromJson(
				"{" +
						"  \"query\": {" +
						"    \"match\": {" +
						"      \"fieldName\": {" +
						"        \"query\": \"test\"" +
						"      }" +
						"    }" +
						"  }," +
						"  \"_source\": false" +
						"}",
				JsonObject.class
		) );

		assertThat(
				new Gson().fromJson(
						index.query()
								.where( f.bool()
										.must( f.match().field( "fieldName" ).matching( "test1" ) )
										.must( f.not( f.match().field( "fieldName" ).matching( "test2" ) ) )
										.mustNot( f.not( f.match().field( "fieldName" ).matching( "test3" ) ) )
										.mustNot( f.matchNone() )
										.toPredicate()
								)
								.toQuery()
								.queryString(),
						JsonObject.class
				)
		).isEqualTo( new Gson().fromJson(
				"{" +
						"  \"query\": {" +
						"    \"bool\": {" +
						"      \"must\": [" +
						"        {" +
						"          \"match\": {" +
						"            \"fieldName\": {" +
						"              \"query\": \"test1\"" +
						"            }" +
						"          }" +
						"        }," +
						"        {" +
						"          \"match\": {" +
						"            \"fieldName\": {" +
						"              \"query\": \"test3\"" +
						"            }" +
						"          }" +
						"        }" +
						"      ]," +
						"      \"must_not\": [" +
						"        {" +
						"          \"match_none\": {}" +
						"        }," +
						"        {" +
						"          \"match\": {" +
						"            \"fieldName\": {" +
						"              \"query\": \"test2\"" +
						"            }" +
						"          }" +
						"        }" +
						"      ]," +
						"      \"minimum_should_match\": \"0\"" +
						"    }" +
						"  }," +
						"  \"_source\": false" +
						"}",
				JsonObject.class
		) );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> field;

		IndexBinding(IndexSchemaElement root) {
			field = root.field( "fieldName", c -> c.asString() ).toReference();
		}
	}
}
