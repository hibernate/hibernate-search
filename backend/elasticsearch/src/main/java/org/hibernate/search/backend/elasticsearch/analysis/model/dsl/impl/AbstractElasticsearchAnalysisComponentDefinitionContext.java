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

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionContributor;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.AnalysisDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisComponentDefinitionContext;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchTypedAnalysisComponentDefinitionContext;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * @author Yoann Rodiere
 */
public abstract class AbstractElasticsearchAnalysisComponentDefinitionContext<D extends AnalysisDefinition>
		implements ElasticsearchTypedAnalysisComponentDefinitionContext,
		ElasticsearchAnalysisComponentDefinitionContext,
		ElasticsearchAnalysisDefinitionContributor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final String name;

	protected final D definition;

	AbstractElasticsearchAnalysisComponentDefinitionContext(String name, D definition) {
		this.name = name;
		this.definition = definition;
	}

	@Override
	public ElasticsearchTypedAnalysisComponentDefinitionContext type(String name) {
		definition.setType( name );
		return this;
	}

	private ElasticsearchTypedAnalysisComponentDefinitionContext param(String name, JsonElement value) {
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
	public ElasticsearchTypedAnalysisComponentDefinitionContext param(String name, String value) {
		return param( name, new JsonPrimitive( value ) );
	}

	@Override
	public ElasticsearchTypedAnalysisComponentDefinitionContext param(String name, String... values) {
		JsonArray array = new JsonArray();
		for ( String value : values ) {
			array.add( value );
		}
		return param( name, array );
	}

	@Override
	public ElasticsearchTypedAnalysisComponentDefinitionContext param(String name, boolean value) {
		return param( name, new JsonPrimitive( value ) );
	}

	@Override
	public ElasticsearchTypedAnalysisComponentDefinitionContext param(String name, boolean... values) {
		JsonArray array = new JsonArray();
		for ( boolean value : values ) {
			array.add( value );
		}
		return param( name, array );
	}

	@Override
	public ElasticsearchTypedAnalysisComponentDefinitionContext param(String name, Number value) {
		return param( name, new JsonPrimitive( value ) );
	}

	@Override
	public ElasticsearchTypedAnalysisComponentDefinitionContext param(String name, Number... values) {
		JsonArray array = new JsonArray();
		for ( Number value : values ) {
			array.add( value );
		}
		return param( name, array );
	}

}
