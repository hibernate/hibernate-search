/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.StringFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.converter.impl.StandardFieldConverter;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.StandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.StandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.StandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.Projectable;
import org.hibernate.search.engine.backend.document.model.dsl.StringIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.util.impl.common.LoggerFactory;

import com.google.gson.JsonElement;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
public class ElasticsearchStringIndexSchemaFieldContextImpl
		extends AbstractElasticsearchStandardIndexSchemaFieldTypedContext<ElasticsearchStringIndexSchemaFieldContextImpl, String>
		implements StringIndexSchemaFieldTypedContext<ElasticsearchStringIndexSchemaFieldContextImpl> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String relativeFieldName;
	private String analyzerName;
	private String normalizerName;
	private Projectable projectable = Projectable.DEFAULT;
	private Sortable sortable = Sortable.DEFAULT;

	public ElasticsearchStringIndexSchemaFieldContextImpl(IndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, String.class );
		this.relativeFieldName = relativeFieldName;
	}

	@Override
	public ElasticsearchStringIndexSchemaFieldContextImpl analyzer(String analyzerName) {
		this.analyzerName = analyzerName;
		return this;
	}

	@Override
	public ElasticsearchStringIndexSchemaFieldContextImpl normalizer(String normalizerName) {
		this.normalizerName = normalizerName;
		return this;
	}

	@Override
	public ElasticsearchStringIndexSchemaFieldContextImpl projectable(Projectable projectable) {
		this.projectable = projectable;
		return this;
	}

	@Override
	public ElasticsearchStringIndexSchemaFieldContextImpl sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected PropertyMapping contribute(IndexSchemaFieldDefinitionHelper<String> helper,
			ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		PropertyMapping mapping = new PropertyMapping();

		StandardFieldConverter<String> converter = new StandardFieldConverter<>(
				helper.createUserIndexFieldConverter(),
				StringFieldCodec.INSTANCE
		);
		ElasticsearchIndexSchemaFieldNode<String> node = new ElasticsearchIndexSchemaFieldNode<>(
				parentNode, converter, StringFieldCodec.INSTANCE,
				new StandardFieldPredicateBuilderFactory( converter ),
				new StandardFieldSortBuilderFactory( converter ),
				new StandardFieldProjectionBuilderFactory( converter )
		);

		JsonAccessor<JsonElement> jsonAccessor = JsonAccessor.root().property( relativeFieldName );
		helper.initialize( new ElasticsearchIndexFieldAccessor<>( jsonAccessor, node ) );
		// TODO Use sub-fields? (but in that case, adjust projections accordingly)
		if ( analyzerName != null ) {
			mapping.setType( DataType.TEXT );
			mapping.setAnalyzer( analyzerName );

			if ( normalizerName != null ) {
				throw log.cannotApplyAnalyzerAndNormalizer( analyzerName, normalizerName, getSchemaContext().getEventContext() );
			}

			switch ( sortable ) {
				case DEFAULT:
				case NO:
					break;
				case YES:
					throw log.cannotUseAnalyzerOnSortableField( analyzerName, getSchemaContext().getEventContext() );
			}
		}
		else {
			mapping.setType( DataType.KEYWORD );
			mapping.setNormalizer( normalizerName );

			switch ( sortable ) {
				case DEFAULT:
					break;
				case NO:
					mapping.setDocValues( false );
					break;
				case YES:
					mapping.setDocValues( true );
					break;
			}
		}

		switch ( projectable ) {
			case DEFAULT:
				break;
			case NO:
				mapping.setStore( false );
				break;
			case YES:
				mapping.setStore( true );
				break;
		}

		String absoluteFieldPath = parentNode.getAbsolutePath( relativeFieldName );
		collector.collect( absoluteFieldPath, node );

		return mapping;
	}

	@Override
	protected ElasticsearchStringIndexSchemaFieldContextImpl thisAsS() {
		return this;
	}
}
