/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchTextFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchStringFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchTextFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchTextFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonPrimitive;


class ElasticsearchStringIndexFieldTypeOptionsStep
		extends AbstractElasticsearchStandardIndexFieldTypeOptionsStep<ElasticsearchStringIndexFieldTypeOptionsStep, String>
		implements StringIndexFieldTypeOptionsStep<ElasticsearchStringIndexFieldTypeOptionsStep> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private String analyzerName;
	private String searchAnalyzerName;
	private String normalizerName;
	private Projectable projectable = Projectable.DEFAULT;
	private Searchable searchable = Searchable.DEFAULT;
	private Norms norms = Norms.DEFAULT;
	private Sortable sortable = Sortable.DEFAULT;
	private Aggregable aggregable = Aggregable.DEFAULT;
	private String indexNullAs;
	private TermVector termVector = TermVector.DEFAULT;

	ElasticsearchStringIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, String.class );
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep analyzer(String analyzerName) {
		this.analyzerName = analyzerName;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep searchAnalyzer(String searchAnalyzerName) {
		this.searchAnalyzerName = searchAnalyzerName;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep normalizer(String normalizerName) {
		this.normalizerName = normalizerName;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep projectable(Projectable projectable) {
		this.projectable = projectable;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep norms(Norms norms) {
		this.norms = norms;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep termVector(TermVector termVector) {
		this.termVector = termVector;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep indexNullAs(String indexNullAs) {
		this.indexNullAs = indexNullAs;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep searchable(Searchable searchable) {
		this.searchable = searchable;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeOptionsStep aggregable(Aggregable aggregable) {
		this.aggregable = aggregable;
		return this;
	}

	@Override
	public IndexFieldType<String> toIndexFieldType() {
		PropertyMapping mapping = new PropertyMapping();

		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );
		boolean resolvedAggregable = resolveDefault( aggregable );

		mapping.setIndex( resolvedSearchable );

		if ( analyzerName != null ) {
			mapping.setType( DataTypes.TEXT );
			mapping.setAnalyzer( analyzerName );
			mapping.setSearchAnalyzer( searchAnalyzerName );
			mapping.setTermVector( resolveTermVector() );

			if ( normalizerName != null ) {
				throw log.cannotApplyAnalyzerAndNormalizer( analyzerName, normalizerName, getBuildContext().getEventContext() );
			}

			if ( resolvedSortable ) {
				throw log.cannotUseAnalyzerOnSortableField( analyzerName, getBuildContext().getEventContext() );
			}

			if ( indexNullAs != null ) {
				throw log.cannotUseIndexNullAsAndAnalyzer( analyzerName, indexNullAs, getBuildContext().getEventContext() );
			}

			if ( resolvedAggregable ) {
				throw log.cannotUseAnalyzerOnAggregableField( analyzerName, getBuildContext().getEventContext() );
			}
		}
		else {
			if ( searchAnalyzerName != null ) {
				throw log.searchAnalyzerWithoutAnalyzer( searchAnalyzerName, getBuildContext().getEventContext() );
			}

			mapping.setType( DataTypes.KEYWORD );
			mapping.setNormalizer( normalizerName );
			mapping.setDocValues( resolvedSortable || resolvedAggregable );
		}

		mapping.setNorms( resolveNorms() );

		if ( indexNullAs != null ) {
			mapping.setNullValue( new JsonPrimitive( indexNullAs ) );
		}

		DslConverter<?, ? extends String> dslConverter = createDslConverter();
		DslConverter<String, ? extends String> rawDslConverter = createRawDslConverter();
		ProjectionConverter<? super String, ?> projectionConverter = createProjectionConverter();
		ProjectionConverter<? super String, String> rawProjectionConverter = createRawProjectionConverter();
		ElasticsearchStringFieldCodec codec = ElasticsearchStringFieldCodec.INSTANCE;

		return new ElasticsearchIndexFieldType<>(
				getFieldType(), dslConverter, projectionConverter,
				codec,
				new ElasticsearchTextFieldPredicateBuilderFactory(
						resolvedSearchable, dslConverter, rawDslConverter, codec, mapping
				),
				new ElasticsearchTextFieldSortBuilderFactory(
						resolvedSortable, dslConverter, rawDslConverter, codec
				),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>(
						resolvedProjectable, projectionConverter, rawProjectionConverter, codec
				),
				new ElasticsearchTextFieldAggregationBuilderFactory(
						resolvedAggregable,
						dslConverter, rawDslConverter,
						projectionConverter, rawProjectionConverter,
						codec,
						analyzerName != null
				),
				mapping,
				analyzerName, searchAnalyzerName != null ? searchAnalyzerName : analyzerName,
				normalizerName
		);
	}

	@Override
	protected ElasticsearchStringIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	private boolean resolveNorms() {
		switch ( norms ) {
			case YES:
				return true;
			case NO:
				return false;
			case DEFAULT:
				return ( analyzerName != null );
			default:
				throw new AssertionFailure( "Unexpected value for Norms: " + norms );
		}
	}

	private String resolveTermVector() {
		switch ( termVector ) {
			case NO:
			case DEFAULT:
				return "no";
			default:
				return termVector.name().toLowerCase( Locale.ROOT );
		}
	}
}
