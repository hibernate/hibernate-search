/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.definition;

import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;

/**
 * A provider of analysis-related definitions that can be referenced from the mapping,
 * e.g. with {@literal @Analyzer(definition = "some-name")}
 * or {@literal @Normalizer(definition = "some-other-name")}.
 * <p>
 * Users can select a definition provider through the
 * {@link ElasticsearchEnvironment#ANALYSIS_DEFINITION_PROVIDER configuration properties}.
 *
 * @author Yoann Rodiere
 *
 * @hsearch.experimental This interface is a prototype.
 * Please let us know what you like and what you don't like, and bear in mind
 * that this will likely change in any future version.
 */
public interface ElasticsearchAnalysisDefinitionProvider {

	void register(ElasticsearchAnalysisDefinitionRegistryBuilder builder);

}
