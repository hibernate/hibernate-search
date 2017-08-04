/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.util.Arrays;
import java.util.Locale;

import org.hibernate.search.backend.elasticsearch.document.impl.DeferredInitializationIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;

/**
 * @author Yoann Rodiere
 */
class LocalDateFieldModelContext extends AbstractScalarFieldModelContext<LocalDate> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.appendValue( YEAR, 4, 9, SignStyle.EXCEEDS_PAD )
			.appendLiteral( '-' )
			.appendValue( MONTH_OF_YEAR, 2 )
			.appendLiteral( '-' )
			.appendValue( DAY_OF_MONTH, 2 )
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	private final JsonAccessor<String> accessor;
	private final DateTimeFormatter formatter = FORMATTER; // TODO add method to allow customization

	public LocalDateFieldModelContext(JsonAccessor<String> accessor) {
		this.accessor = accessor;
	}

	@Override
	protected void build(DeferredInitializationIndexFieldReference<LocalDate> reference, PropertyMapping mapping) {
		super.build( reference, mapping );
		reference.initialize( new FormattingElasticsearchIndexFieldReference( accessor, formatter ) );
		mapping.setType( DataType.DATE );
		mapping.setFormat( Arrays.asList( "strict_date", "yyyyyyyyy-MM-dd" ) );
	}

	private static class FormattingElasticsearchIndexFieldReference extends ElasticsearchIndexFieldReference<LocalDate, String> {

		private final DateTimeFormatter formatter;

		protected FormattingElasticsearchIndexFieldReference(JsonAccessor<String> accessor, DateTimeFormatter formatter) {
			super( accessor );
			this.formatter = formatter;
		}

		@Override
		protected String convert(LocalDate value) {
			return value == null ? null : formatter.format( value );
		}

	}
}
