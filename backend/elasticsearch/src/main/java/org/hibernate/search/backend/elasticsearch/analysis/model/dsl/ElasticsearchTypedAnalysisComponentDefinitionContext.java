/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl;


public interface ElasticsearchTypedAnalysisComponentDefinitionContext {

	/**
	 * Set the value of a parameter to a given string.
	 * <p>
	 * Supported parameters depend on the {@link ElasticsearchAnalysisComponentDefinitionContext#type(String) type}
	 * being used.
	 *
	 * @param name The name of the parameter.
	 * @param value The value of the parameter.
	 * @return This context, allowing to chain calls.
	 */
	ElasticsearchTypedAnalysisComponentDefinitionContext param(String name, String value);

	/**
	 * Set the value of a parameter to a given array of strings.
	 * <p>
	 * Supported parameters depend on the {@link ElasticsearchAnalysisComponentDefinitionContext#type(String) type}
	 * being used.
	 *
	 * @param name The name of the parameter.
	 * @param values The value of the parameter.
	 * @return This context, allowing to chain calls.
	 */
	ElasticsearchTypedAnalysisComponentDefinitionContext param(String name, String... values);

	/**
	 * Set the value of a parameter to a given boolean.
	 * <p>
	 * Supported parameters depend on the {@link ElasticsearchAnalysisComponentDefinitionContext#type(String) type}
	 * being used.
	 *
	 * @param name The name of the parameter.
	 * @param value The value of the parameter.
	 * @return This context, allowing to chain calls.
	 */
	ElasticsearchTypedAnalysisComponentDefinitionContext param(String name, boolean value);

	/**
	 * Set the value of a parameter to a given array of booleans.
	 * <p>
	 * Supported parameters depend on the {@link ElasticsearchAnalysisComponentDefinitionContext#type(String) type}
	 * being used.
	 *
	 * @param name The name of the parameter.
	 * @param values The value of the parameter.
	 * @return This context, allowing to chain calls.
	 */
	ElasticsearchTypedAnalysisComponentDefinitionContext param(String name, boolean... values);

	/**
	 * Set the value of a parameter to a given number (int, long, float, double, ...).
	 * <p>
	 * Supported parameters depend on the {@link ElasticsearchAnalysisComponentDefinitionContext#type(String) type}
	 * being used.
	 *
	 * @param name The name of the parameter.
	 * @param value The value of the parameter.
	 * @return This context, allowing to chain calls.
	 */
	ElasticsearchTypedAnalysisComponentDefinitionContext param(String name, Number value);

	/**
	 * Set the value of a parameter to a given array of numbers (int, long, float, double, ...).
	 * <p>
	 * Supported parameters depend on the {@link ElasticsearchAnalysisComponentDefinitionContext#type(String) type}
	 * being used.
	 *
	 * @param name The name of the parameter.
	 * @param values The value of the parameter.
	 * @return This context, allowing to chain calls.
	 */
	ElasticsearchTypedAnalysisComponentDefinitionContext param(String name, Number... values);

}
