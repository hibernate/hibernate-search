/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl;


/**
 * The step in an analysis component definition where the type of that component can be set.
 */
public interface ElasticsearchAnalysisComponentTypeStep {

	/**
	 * Set the Elasticsearch type to use for the definition.
	 *
	 * @param name The value for the 'type' parameter in the Elasticsearch definition.
	 * @return The next step.
	 */
	ElasticsearchAnalysisComponentParametersStep type(String name);

}
