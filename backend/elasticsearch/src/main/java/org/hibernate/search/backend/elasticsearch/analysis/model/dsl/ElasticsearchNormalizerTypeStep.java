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
