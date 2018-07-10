/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.time.LocalDate;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.util.Arrays;
import java.util.Locale;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.backend.document.spi.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.LocalDateFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.StandardFieldPredicateBuilderFactory;

import com.google.gson.JsonElement;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
public class LocalDateIndexSchemaFieldContext extends AbstractScalarFieldTypedContext<LocalDate> {

	private static final LocalDateFieldCodec DEFAULT_CODEC = new LocalDateFieldCodec(
					new DateTimeFormatterBuilder()
							.appendValue( YEAR, 4, 9, SignStyle.EXCEEDS_PAD )
							.appendLiteral( '-' )
							.appendValue( MONTH_OF_YEAR, 2 )
							.appendLiteral( '-' )
							.appendValue( DAY_OF_MONTH, 2 )
							.toFormatter( Locale.ROOT )
							.withResolverStyle( ResolverStyle.STRICT )
			);

	private final String relativeFieldName;
	private final LocalDateFieldCodec codec = DEFAULT_CODEC; // TODO add method to allow customization

	public LocalDateIndexSchemaFieldContext(IndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, relativeFieldName, DataType.DATE );
		this.relativeFieldName = relativeFieldName;
	}

	@Override
	protected PropertyMapping contribute(DeferredInitializationIndexFieldAccessor<LocalDate> reference,
			ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		PropertyMapping mapping = super.contribute( reference, collector, parentNode );

		ElasticsearchIndexSchemaFieldNode node = new ElasticsearchIndexSchemaFieldNode( parentNode, codec, new StandardFieldPredicateBuilderFactory( codec ) );

		JsonAccessor<JsonElement> jsonAccessor = JsonAccessor.root().property( relativeFieldName );
		reference.initialize( new ElasticsearchIndexFieldAccessor<>( jsonAccessor, node ) );
		mapping.setFormat( Arrays.asList( "strict_date", "yyyyyyyyy-MM-dd" ) );

		String absoluteFieldPath = parentNode.getAbsolutePath( relativeFieldName );

		collector.collect( absoluteFieldPath, node );

		return mapping;
	}
}
