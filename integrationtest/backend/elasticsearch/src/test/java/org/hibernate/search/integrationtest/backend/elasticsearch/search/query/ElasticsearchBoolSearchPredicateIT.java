/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.search.query;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEqualsIgnoringUnknownFields;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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

		assertJsonEqualsIgnoringUnknownFields(
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
						"  }" +
						"}",
				index.query()
						.where( f.bool().must( f.not( f.not( f.not( f.not( f.not( f.not( f.not( f.match().field( "fieldName" ).matching( "test" ) ) ) ) ) ) ) ) ).toPredicate() )
						.toQuery()
						.queryString()
		);

		assertJsonEqualsIgnoringUnknownFields(
				"{" +
						"  \"query\": {" +
						"    \"match\": {" +
						"      \"fieldName\": {" +
						"        \"query\": \"test\"" +
						"      }" +
						"    }" +
						"  }" +
						"}",
				index.query()
						.where( f.not( f.bool().must( f.not( f.not( f.not( f.not( f.not( f.not( f.not( f.match().field( "fieldName" ).matching( "test" ) ) ) ) ) ) ) ) ) ).toPredicate() )
						.toQuery()
						.queryString()
		);

		assertJsonEqualsIgnoringUnknownFields(
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
						"  }" +
						"}",
				index.query()
						.where( f.bool()
								.must( f.match().field( "fieldName" ).matching( "test1" ) )
								.must( f.not( f.match().field( "fieldName" ).matching( "test2" ) ) )
								.mustNot( f.not( f.match().field( "fieldName" ).matching( "test3" ) ) )
								.mustNot( f.matchNone() )
								.toPredicate()
						)
						.toQuery()
						.queryString()
		);
	}

	@Test
	public void resultingQueryOptimizationWithBoost() {
		SearchPredicateFactory f = index.createScope().predicate();

		assertJsonEqualsIgnoringUnknownFields(
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
						"  }" +
						"}",
				index.query()
						.where( f.not( f.match().field( "fieldName" ).matching( "test" ) ).toPredicate() )
						.toQuery()
						.queryString()
		);

		// having boost in the not predicate should result in having additional must{match_all{}}
		assertJsonEqualsIgnoringUnknownFields(
				"{" +
						"  \"query\": {" +
						"    \"bool\": {" +
						"      \"boost\": 5.0," +
						"      \"must_not\": {" +
						"        \"match\": {" +
						"          \"fieldName\": {" +
						"            \"query\": \"test\"" +
						"          }" +
						"        }" +
						"      }," +
						"      \"must\": {" +
						"        \"match_all\": {}" +
						"      }" +
						"    }" +
						"  }" +
						"}",
				index.query()
						.where( f.not( f.match().field( "fieldName" ).matching( "test" ) ).boost( 5.0F ).toPredicate() )
						.toQuery()
						.queryString()
		);
	}

	@Test
	public void nested() {
		String expectedQueryJson = "{" +
				"  \"query\": {" +
				"    \"bool\": {" +
				"      \"must\": [" +
				"        {" +
				"          \"match\": {" +
				"            \"fieldName\": {" +
				"              \"query\": \"test\"" +
				"            }" +
				"          }" +
				"        }," +
				"        {" +
				"          \"nested\": {" +
				"            \"path\": \"nested\"," +
				"            \"query\": {" +
				"              \"bool\": {" +
				"                \"must\": [" +
				"                  {" +
				"                    \"range\": {" +
				"                      \"nested.integer\": {" +
				"                        \"gte\": 5," +
				"                        \"lte\": 10" +
				"                      }" +
				"                    }" +
				"                  }," +
				"                  {" +
				"                    \"match\": {" +
				"                      \"nested.text\": {" +
				"                        \"query\": \"value\"" +
				"                      }" +
				"                    }" +
				"                  }" +
				"                ]" +
				"              }" +
				"            }" +
				"          }" +
				"        }" +
				"      ]" +
				"    }" +
				"  }" +
				"}";

		assertJsonEqualsIgnoringUnknownFields(
				expectedQueryJson,
				index.query().where( f -> f.bool()
								.must( f.match().field( "fieldName" ).matching( "test" ) )
								.must( f.nested( "nested" )
										.add( f.range().field( "nested.integer" )
												.between( 5, 10 ) )
										.add( f.match().field( "nested.text" )
												.matching( "value" )
										) )
						).toQuery()
						.queryString()
		);

		// now the same query but using the bool predicate instead:
		assertJsonEqualsIgnoringUnknownFields(
				expectedQueryJson,
				index.query().where( f -> f.bool()
								.must( f.match().field( "fieldName" ).matching( "test" ) )
								.must( f.nested( "nested" )
										.add(
												f.bool()
														.must( f.range().field( "nested.integer" )
																.between( 5, 10 ) )
														.must( f.match().field( "nested.text" )
																.matching( "value" )
														)
										) )
						).toQuery()
						.queryString()
		);
	}

	@Test
	public void onlyNested() {
		//bool query remains as there are > 1 clause
		assertJsonEqualsIgnoringUnknownFields(
				"{" +
						"  \"query\": {" +
						"    \"nested\": {" +
						"      \"path\": \"nested\"," +
						"      \"query\": {" +
						"        \"bool\": {" +
						"          \"must\": [" +
						"            {" +
						"              \"range\": {" +
						"                \"nested.integer\": {" +
						"                  \"gte\": 5," +
						"                  \"lte\": 10" +
						"                }" +
						"              }" +
						"            }," +
						"            {" +
						"              \"match\": {" +
						"                \"nested.text\": {" +
						"                  \"query\": \"value\"" +
						"                }" +
						"              }" +
						"            }" +
						"          ]" +
						"        }" +
						"      }" +
						"    }" +
						"  }" +
						"}",
				index.query().where( f -> f.nested( "nested" )
								.add(
										f.bool()
												.must( f.range().field( "nested.integer" )
														.between( 5, 10 )
												)
												.must( f.match().field( "nested.text" )
														.matching( "value" )
												)
								)
						).toQuery()
						.queryString()
		);

		// bool query is removed as there's only 1 clause
		assertJsonEqualsIgnoringUnknownFields(
				"{" +
						"  \"query\": {" +
						"    \"nested\": {" +
						"      \"path\": \"nested\"," +
						"      \"query\": {" +
						"        \"range\": {" +
						"          \"nested.integer\": {" +
						"            \"gte\": 5," +
						"            \"lte\": 10" +
						"          }" +
						"        }" +
						"      }" +
						"    }" +
						"  }" +
						"}",
				index.query().where( f -> f.nested( "nested" )
								.add(
										f.bool()
												.must( f.range().field( "nested.integer" )
														.between( 5, 10 ) )
								)
						).toQuery()
						.queryString()
		);
	}

	private static class IndexBinding {
		final IndexFieldReference<String> field;

		IndexBinding(IndexSchemaElement root) {
			field = root.field( "fieldName", c -> c.asString() ).toReference();
			IndexSchemaObjectField nested = root.objectField( "nested", ObjectStructure.NESTED );
			nested.toReference();

			nested.field( "text", c -> c.asString() ).toReference();
			nested.field( "integer", c -> c.asInteger() ).toReference();
		}
	}
}
