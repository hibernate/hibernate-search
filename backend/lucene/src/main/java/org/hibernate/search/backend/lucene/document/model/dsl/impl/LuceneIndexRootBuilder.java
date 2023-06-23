/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.document.model.impl.AbstractLuceneIndexFieldTemplate;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexObjectField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexObjectFieldTemplate;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexRoot;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexValueField;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexValueFieldTemplate;
import org.hibernate.search.backend.lucene.types.dsl.LuceneIndexFieldTypeFactory;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneIndexFieldTypeFactoryImpl;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexCompositeNodeType;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexRootBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexIdentifier;
import org.hibernate.search.engine.backend.mapping.spi.BackendMapperContext;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;

public class LuceneIndexRootBuilder extends AbstractLuceneIndexCompositeNodeBuilder
		implements IndexRootBuilder, IndexSchemaBuildContext {

	private final EventContext indexEventContext;
	private final BackendMapperContext backendMapperContext;
	private final String mappedTypeName;
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

	private DslConverter<?, String> idDslConverter;
	private ProjectionConverter<String, ?> idProjectionConverter;

	public LuceneIndexRootBuilder(EventContext indexEventContext,
			BackendMapperContext backendMapperContext, String mappedTypeName,
			LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry) {
		super( new LuceneIndexCompositeNodeType.Builder( ObjectStructure.FLATTENED ) );
		this.indexEventContext = indexEventContext;
		this.backendMapperContext = backendMapperContext;
		this.mappedTypeName = mappedTypeName;
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
	}

	@Override
	public EventContext eventContext() {
		return getIndexEventContext().append( EventContexts.indexSchemaRoot() );
	}

	@Override
	public LuceneIndexFieldTypeFactory createTypeFactory(IndexFieldTypeDefaultsProvider defaultsProvider) {
		return new LuceneIndexFieldTypeFactoryImpl( indexEventContext, backendMapperContext, analysisDefinitionRegistry,
				defaultsProvider );
	}

	@Override
	public void explicitRouting() {
		// Nothing to do
	}

	@Override
	public <I> void idDslConverter(Class<I> valueType, ToDocumentValueConverter<I, String> converter) {
		this.idDslConverter = new DslConverter<>( valueType, converter );
	}

	@Override
	public <I> void idProjectionConverter(Class<I> valueType, FromDocumentValueConverter<String, I> converter) {
		this.idProjectionConverter = new ProjectionConverter<>( valueType, converter );
	}

	@Override
	public LuceneIndexRootBuilder getRootNodeBuilder() {
		return this;
	}

	public LuceneIndexModel build(String indexName) {
		IndexIdentifier identifier = new IndexIdentifier( idDslConverter, idProjectionConverter );

		Map<String, LuceneIndexField> staticFields = new HashMap<>();
		List<AbstractLuceneIndexFieldTemplate<?>> fieldTemplates = new ArrayList<>();
		// Initializing a one-element array so that we can mutate the boolean below.
		// Alternatively we could use AtomicBoolean, but we don't need concurrent access here.
		boolean[] hasNestedDocument = new boolean[1];

		LuceneIndexNodeCollector collector = new LuceneIndexNodeCollector() {
			@Override
			public void collect(String absoluteFieldPath, LuceneIndexValueField<?> node) {
				staticFields.put( absoluteFieldPath, node );
			}

			@Override
			public void collect(String absolutePath, LuceneIndexObjectField node) {
				staticFields.put( absolutePath, node );
				if ( node.type().nested() ) {
					hasNestedDocument[0] = true;
				}
			}

			@Override
			public void collect(LuceneIndexObjectFieldTemplate template) {
				fieldTemplates.add( template );
				if ( template.type().nested() ) {
					hasNestedDocument[0] = true;
				}
			}

			@Override
			public void collect(LuceneIndexValueFieldTemplate template) {
				fieldTemplates.add( template );
			}
		};

		Map<String, LuceneIndexField> staticChildrenByName = new TreeMap<>();
		LuceneIndexRoot rootNode = new LuceneIndexRoot( typeBuilder.build(), staticChildrenByName );
		contributeChildren( rootNode, collector, staticChildrenByName );

		return new LuceneIndexModel( indexName, mappedTypeName, identifier,
				rootNode, staticFields, fieldTemplates, hasNestedDocument[0] );
	}

	@Override
	String getAbsolutePath() {
		return null;
	}

	EventContext getIndexEventContext() {
		return indexEventContext;
	}

}
