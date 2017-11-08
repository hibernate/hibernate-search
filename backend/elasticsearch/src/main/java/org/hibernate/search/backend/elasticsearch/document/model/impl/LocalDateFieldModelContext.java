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

import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementType;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnknownTypeJsonAccessor;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
class LocalDateFieldModelContext extends AbstractScalarFieldModelContext<LocalDate> {

	private static final LocalDateFormatter DEFAULT_FORMATTER = new LocalDateFormatter(
					new DateTimeFormatterBuilder()
							.appendValue( YEAR, 4, 9, SignStyle.EXCEEDS_PAD )
							.appendLiteral( '-' )
							.appendValue( MONTH_OF_YEAR, 2 )
							.appendLiteral( '-' )
							.appendValue( DAY_OF_MONTH, 2 )
							.toFormatter( Locale.ROOT )
							.withResolverStyle( ResolverStyle.STRICT )
			);

	private final UnknownTypeJsonAccessor accessor;
	private final LocalDateFormatter formatter = DEFAULT_FORMATTER; // TODO add method to allow customization

	public LocalDateFieldModelContext(UnknownTypeJsonAccessor accessor) {
		this.accessor = accessor;
	}

	@Override
	protected PropertyMapping contribute(DeferredInitializationIndexFieldReference<LocalDate> reference,
			ElasticsearchFieldModelCollector collector) {
		PropertyMapping mapping = super.contribute( reference, collector );

		ElasticsearchFieldModel model = new ElasticsearchFieldModel( formatter );

		reference.initialize( new ElasticsearchIndexFieldReference<>( accessor, model ) );
		mapping.setType( DataType.DATE );
		mapping.setFormat( Arrays.asList( "strict_date", "yyyyyyyyy-MM-dd" ) );

		String absolutePath = accessor.getStaticAbsolutePath();
		collector.collect( absolutePath, model );

		return mapping;
	}

	private static final class LocalDateFormatter implements ElasticsearchFieldFormatter {

		private final DateTimeFormatter delegate;

		protected LocalDateFormatter(DateTimeFormatter delegate) {
			this.delegate = delegate;
		}

		@Override
		public JsonElement format(Object object) {
			if ( object == null ) {
				return JsonNull.INSTANCE;
			}
			LocalDate value = (LocalDate) object;
			return new JsonPrimitive( delegate.format( value ) );
		}

		@Override
		public Object parse(JsonElement element) {
			if ( element == null || element.isJsonNull() ) {
				return null;
			}
			String stringValue = JsonElementType.STRING.fromElement( element );
			return LocalDate.parse( stringValue, delegate );
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == null || obj.getClass() != getClass() ) {
				return false;
			}
			LocalDateFormatter other = (LocalDateFormatter) obj;
			return delegate.equals( other.delegate );
		}

		@Override
		public int hashCode() {
			return delegate.hashCode();
		}

	}
}
