/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchStringFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchTextFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonPrimitive;


class ElasticsearchStringIndexFieldTypeContext
		extends AbstractElasticsearchStandardIndexFieldTypeContext<ElasticsearchStringIndexFieldTypeContext, String>
		implements StringIndexFieldTypeContext<ElasticsearchStringIndexFieldTypeContext> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private String analyzerName;
	private String normalizerName;
	private Projectable projectable = Projectable.DEFAULT;
	private Searchable searchable = Searchable.DEFAULT;
	private Norms norms = Norms.DEFAULT;
	private Sortable sortable = Sortable.DEFAULT;
	private String indexNullAs;
	private TermVector termVector = TermVector.DEFAULT;

	ElasticsearchStringIndexFieldTypeContext(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, String.class );
	}

	@Override
	public ElasticsearchStringIndexFieldTypeContext analyzer(String analyzerName) {
		this.analyzerName = analyzerName;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeContext normalizer(String normalizerName) {
		this.normalizerName = normalizerName;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeContext projectable(Projectable projectable) {
		this.projectable = projectable;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeContext norms(Norms norms) {
		this.norms = norms;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeContext termVector(TermVector termVector) {
		this.termVector = termVector;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeContext indexNullAs(String indexNullAs) {
		this.indexNullAs = indexNullAs;
		return this;
	}

	@Override
	public ElasticsearchStringIndexFieldTypeContext searchable(Searchable searchable) {
		this.searchable = searchable;
		return this;
	}

	@Override
	public IndexFieldType<String> toIndexFieldType() {
		PropertyMapping mapping = new PropertyMapping();

		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );

		mapping.setIndex( resolvedSearchable );

		if ( analyzerName != null ) {
			mapping.setType( DataType.TEXT );
			mapping.setAnalyzer( analyzerName );
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
		}
		else {
			mapping.setType( DataType.KEYWORD );
			mapping.setNormalizer( normalizerName );
			mapping.setDocValues( resolvedSortable );
		}

		mapping.setStore( resolvedProjectable );
		mapping.setNorms( resolveNorms() );

		if ( indexNullAs != null ) {
			mapping.setNullValue( new JsonPrimitive( indexNullAs ) );
		}

		ToDocumentFieldValueConverter<?, ? extends String> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super String, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		ElasticsearchStringFieldCodec codec = ElasticsearchStringFieldCodec.INSTANCE;

		return new ElasticsearchIndexFieldType<>(
				codec,
				new ElasticsearchTextFieldPredicateBuilderFactory( resolvedSearchable, dslToIndexConverter, createToDocumentRawConverter(), codec, mapping ),
				new ElasticsearchStandardFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, createFromDocumentRawConverter(), codec ),
				mapping
		);
	}

	@Override
	protected ElasticsearchStringIndexFieldTypeContext thisAsS() {
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
