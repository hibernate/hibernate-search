/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.io.IOException;

import org.hibernate.search.elasticsearch.testutil.junit.SkipBelowElasticsearch50;
import org.junit.experimental.categories.Category;

/**
 * @author Yoann Rodiere
 */
@Category(SkipBelowElasticsearch50.class)
public class Elasticsearch5AnalyzerDefinitionValidationIT extends AbstractAnalyzerDefinitionValidationIT {

	@Override
	protected void putMapping() throws IOException {
		elasticSearchClient.type( AnalyzedEntity.class ).putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'analyzer': 'analyzerWithElasticsearchFactories'"
							+ "}"
					+ "}"
				+ "}"
				);
	}

}
