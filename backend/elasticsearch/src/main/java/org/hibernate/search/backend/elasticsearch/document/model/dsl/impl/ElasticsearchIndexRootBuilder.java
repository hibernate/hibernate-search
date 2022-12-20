/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexFieldTemplate;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexObjectField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexRoot;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexObjectFieldTemplate;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexValueFieldTemplate;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexValueField;
import org.hibernate.search.backend.elasticsearch.index.DynamicMapping;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RoutingType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactory;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexCompositeNodeType;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.document.model.dsl.spi.ImplicitFieldCollector;
import org.hibernate.search.engine.backend.document.model.dsl.spi.ImplicitFieldContributor;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexRootBuilder;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.document.model.spi.IndexIdentifier;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;

public class ElasticsearchIndexRootBuilder extends AbstractElasticsearchIndexCompositeNodeBuilder
		implements IndexRootBuilder {

	private final ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider;
	private final EventContext indexEventContext;
	private final List<IndexSchemaRootContributor> schemaRootContributors = new ArrayList<>();
	private final List<ImplicitFieldContributor> implicitFieldContributors = new ArrayList<>();
	private final IndexNames indexNames;
	private final String mappedTypeName;
	private final ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private final IndexSettings customIndexSettings;
	private final RootTypeMapping customIndexMapping;
	private final DynamicType defaultDynamicType;

	private RoutingType routing = null;
	private DslConverter<?, String> idDslConverter;
	private ProjectionConverter<String, ?> idProjectionConverter;

	public ElasticsearchIndexRootBuilder(ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider,
			EventContext indexEventContext,
			IndexNames indexNames, String mappedTypeName,
			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry,
			IndexSettings customIndexSettings, RootTypeMapping customIndexMapping,
			DynamicMapping dynamicMapping) {
		super( new ElasticsearchIndexCompositeNodeType.Builder( ObjectStructure.FLATTENED ) );
		this.typeFactoryProvider = typeFactoryProvider;
		this.indexEventContext = indexEventContext;
		this.indexNames = indexNames;
		this.mappedTypeName = mappedTypeName;
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.customIndexSettings = customIndexSettings;
		this.customIndexMapping = customIndexMapping;
		this.defaultDynamicType = DynamicType.create( dynamicMapping );

		this.addDefaultImplicitFields();
	}

	@Override
	public EventContext eventContext() {
		return getIndexEventContext()
				.append( EventContexts.indexSchemaRoot() );
	}

	@Override
	public ElasticsearchIndexFieldTypeFactory createTypeFactory(IndexFieldTypeDefaultsProvider defaultsProvider) {
		return typeFactoryProvider.create( indexEventContext, defaultsProvider );
	}

	@Override
	public void explicitRouting() {
		this.routing = RoutingType.REQUIRED;
	}

	@Override
	public <I> void idDslConverter(Class<I> valueType, ToDocumentValueConverter<I, String> converter) {
		this.idDslConverter = new DslConverter<>( valueType, converter );
	}

	@Override
	public <I> void idProjectionConverter(Class<I> valueType, FromDocumentValueConverter<String, I> converter) {
		this.idProjectionConverter = new ProjectionConverter<>( valueType, converter );
	}

	public void addSchemaRootContributor(IndexSchemaRootContributor schemaRootContributor) {
		schemaRootContributors.add( schemaRootContributor );
	}

	public void addImplicitFieldContributor(ImplicitFieldContributor implicitFieldContributor) {
		implicitFieldContributors.add( implicitFieldContributor );
	}

	public ElasticsearchIndexModel build() {
		IndexIdentifier identifier = new IndexIdentifier( idDslConverter, idProjectionConverter );

		RootTypeMapping mapping = new RootTypeMapping();
		if ( routing != null ) {
			mapping.setRouting( routing );
		}

		for ( IndexSchemaRootContributor schemaRootContributor : schemaRootContributors ) {
			schemaRootContributor.contribute( mapping );
		}

		mapping.setDynamic( resolveSelfDynamicType( defaultDynamicType ) );

		Map<String, ElasticsearchIndexField> staticFields = new HashMap<>();
		List<AbstractElasticsearchIndexFieldTemplate<?>> fieldTemplates = new ArrayList<>();

		ElasticsearchIndexNodeCollector collector = new ElasticsearchIndexNodeCollector() {
			@Override
			public void collect(String absolutePath, ElasticsearchIndexObjectField node) {
				staticFields.put( absolutePath, node );
			}

			@Override
			public void collect(String absoluteFieldPath, ElasticsearchIndexValueField<?> node) {
				staticFields.put( absoluteFieldPath, node );
			}

			@Override
			public void collect(ElasticsearchIndexObjectFieldTemplate template) {
				fieldTemplates.add( template );
			}

			@Override
			public void collect(ElasticsearchIndexValueFieldTemplate template) {
				fieldTemplates.add( template );
			}

			@Override
			public void collect(NamedDynamicTemplate templateForMapping) {
				mapping.addDynamicTemplate( templateForMapping );
			}
		};

		Map<String, ElasticsearchIndexField> staticChildrenByName = new TreeMap<>();
		ElasticsearchIndexRoot rootNode = new ElasticsearchIndexRoot( typeBuilder.build(), staticChildrenByName );
		contributeChildren( mapping, rootNode, collector, staticChildrenByName );

		ImplicitFieldCollector implicitFieldCollector = new ImplicitFieldCollector() {

			private ElasticsearchIndexFieldTypeFactory typeFactory = typeFactoryProvider.create(
					indexEventContext,
					new IndexFieldTypeDefaultsProvider()
			);

			@Override
			public IndexFieldTypeFactory indexFieldTypeFactory() {
				return typeFactory;
			}

			@Override
			public <F> void addImplicitField(String fieldName, IndexFieldType<F> indexFieldType) {
				ElasticsearchIndexValueFieldType<F> fieldType = (ElasticsearchIndexValueFieldType<F>) indexFieldType;

				staticFields.put(
						fieldName,
						new ElasticsearchIndexValueField<>(
								rootNode, fieldName, fieldType, IndexFieldInclusion.INCLUDED, false )
				);
			}
		};
		for ( ImplicitFieldContributor contributor : implicitFieldContributors ) {
			contributor.contribute( implicitFieldCollector );
		}

		return new ElasticsearchIndexModel( indexNames, mappedTypeName, identifier,
				rootNode, staticFields, fieldTemplates,
				analysisDefinitionRegistry, customIndexSettings, mapping, customIndexMapping );
	}

	@Override
	ElasticsearchIndexRootBuilder getRootNodeBuilder() {
		return this;
	}

	@Override
	String getAbsolutePath() {
		return null;
	}

	EventContext getIndexEventContext() {
		return indexEventContext;
	}

	private void addDefaultImplicitFields() {
		implicitFieldContributors.add( new ElasticsearchStringImplicitFieldContributor( "_id" ) );
		implicitFieldContributors.add( new ElasticsearchStringImplicitFieldContributor( "_index" ) );
	}
}
