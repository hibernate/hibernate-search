/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.definition.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.elasticsearch.analyzer.definition.ElasticsearchAnalysisComponentDefinitionContext;
import org.hibernate.search.elasticsearch.analyzer.definition.ElasticsearchTypedAnalysisComponentDefinitionContext;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalysisDefinition;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * @author Yoann Rodiere
 */
public abstract class ElasticsearchAnalysisComponentDefinitionContextImpl<D extends AnalysisDefinition>
		implements ElasticsearchTypedAnalysisComponentDefinitionContext,
				ElasticsearchAnalysisComponentDefinitionContext,
				ElasticsearchAnalysisDefinitionRegistryPopulator {

	protected static final Log LOG = LoggerFactory.make( Log.class );

	protected final String name;

	protected final D definition;

	protected ElasticsearchAnalysisComponentDefinitionContextImpl(String name, D definition) {
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
		parameters.put( name, value );
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
