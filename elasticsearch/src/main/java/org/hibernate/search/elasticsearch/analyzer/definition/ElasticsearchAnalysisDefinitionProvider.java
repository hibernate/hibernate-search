/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.definition;

import org.hibernate.search.annotations.Factory;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;

/**
 * A provider of analyzer definitions that can be referenced from the mapping,
 * e.g. with {@literal @Analyzer(definition = "some-name")}.
 * <p>
 * Implementors should define a concrete class with either a public default constructor
 * or a public static method annotated with {@link Factory}.
 * <p>
 * Users can select a definition provider through the
 * {@link ElasticsearchEnvironment#ANALYZER_DEFINITION_PROVIDER configuration properties}.
 *
 * @author Yoann Rodiere
 */
public interface ElasticsearchAnalysisDefinitionProvider {

	void register(ElasticsearchAnalysisDefinitionRegistryBuilder builder);

}
