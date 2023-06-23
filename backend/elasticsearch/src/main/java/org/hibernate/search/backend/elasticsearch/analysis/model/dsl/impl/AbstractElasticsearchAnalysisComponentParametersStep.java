/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisComponentParametersStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisComponentTypeStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionContributor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalysisDefinition;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

abstract class AbstractElasticsearchAnalysisComponentParametersStep<D extends AnalysisDefinition>
		implements ElasticsearchAnalysisComponentParametersStep,
		ElasticsearchAnalysisComponentTypeStep,
		ElasticsearchAnalysisDefinitionContributor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final String name;

	protected final D definition;

	AbstractElasticsearchAnalysisComponentParametersStep(String name, D definition) {
		this.name = name;
		this.definition = definition;
	}

	@Override
	public ElasticsearchAnalysisComponentParametersStep type(String name) {
		definition.setType( name );
		return this;
	}

	private ElasticsearchAnalysisComponentParametersStep param(String name, JsonElement value) {
		Map<String, JsonElement> parameters = definition.getParameters();
		if ( parameters == null ) {
			parameters = new LinkedHashMap<>();
			definition.setParameters( parameters );
		}
		JsonElement previous = parameters.putIfAbsent( name, value );
		if ( previous != null ) {
			throw log.analysisComponentParameterConflict( name, previous, value );
		}
		return this;
	}

	@Override
	public ElasticsearchAnalysisComponentParametersStep param(String name, String value) {
		return param( name, new JsonPrimitive( value ) );
	}

	@Override
	public ElasticsearchAnalysisComponentParametersStep param(String name, String... values) {
		JsonArray array = new JsonArray();
		for ( String value : values ) {
			array.add( value );
		}
		return param( name, array );
	}

	@Override
	public ElasticsearchAnalysisComponentParametersStep param(String name, boolean value) {
		return param( name, new JsonPrimitive( value ) );
	}

	@Override
	public ElasticsearchAnalysisComponentParametersStep param(String name, boolean... values) {
		JsonArray array = new JsonArray();
		for ( boolean value : values ) {
			array.add( value );
		}
		return param( name, array );
	}

	@Override
	public ElasticsearchAnalysisComponentParametersStep param(String name, Number value) {
		return param( name, new JsonPrimitive( value ) );
	}

	@Override
	public ElasticsearchAnalysisComponentParametersStep param(String name, Number... values) {
		JsonArray array = new JsonArray();
		for ( Number value : values ) {
			array.add( value );
		}
		return param( name, array );
	}

}
