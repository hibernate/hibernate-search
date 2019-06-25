/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl;


/**
 * The initial step in an analyzer definition, where the type of normalizer can be set.
 */
public interface ElasticsearchNormalizerTypeStep {

	/**
	 * Start a custom normalizer definition,
	 * assigning char filters and token filters to the definition.
	 *
	 * @return The next step.
	 */
	ElasticsearchNormalizerOptionalComponentsStep custom();

}
