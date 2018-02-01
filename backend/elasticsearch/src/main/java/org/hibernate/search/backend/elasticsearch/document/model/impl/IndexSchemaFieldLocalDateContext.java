/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.util.Arrays;
import java.util.Locale;

import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementType;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
class IndexSchemaFieldLocalDateContext extends AbstractScalarFieldTypedContext<LocalDate> {

	private static final LocalDateFieldFormatter DEFAULT_FORMATTER = new LocalDateFieldFormatter(
					new DateTimeFormatterBuilder()
							.appendValue( YEAR, 4, 9, SignStyle.EXCEEDS_PAD )
							.appendLiteral( '-' )
							.appendValue( MONTH_OF_YEAR, 2 )
							.appendLiteral( '-' )
							.appendValue( DAY_OF_MONTH, 2 )
							.toFormatter( Locale.ROOT )
							.withResolverStyle( ResolverStyle.STRICT )
			);

	private final String relativeName;
	private final LocalDateFieldFormatter formatter = DEFAULT_FORMATTER; // TODO add method to allow customization

	public IndexSchemaFieldLocalDateContext(String relativeName) {
		this.relativeName = relativeName;
	}

	@Override
	protected PropertyMapping contribute(DeferredInitializationIndexFieldAccessor<LocalDate> reference,
			ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		PropertyMapping mapping = super.contribute( reference, collector, parentNode );

		ElasticsearchIndexSchemaFieldNode node = new ElasticsearchIndexSchemaFieldNode( parentNode, formatter );

		JsonAccessor<JsonElement> jsonAccessor = JsonAccessor.root().property( relativeName );
		reference.initialize( new ElasticsearchIndexFieldAccessor<>( jsonAccessor, node ) );
		mapping.setType( DataType.DATE );
		mapping.setFormat( Arrays.asList( "strict_date", "yyyyyyyyy-MM-dd" ) );

		String absolutePath = parentNode.getAbsolutePath( relativeName );
		collector.collect( absolutePath, node );

		return mapping;
	}

	private static final class LocalDateFieldFormatter implements ElasticsearchFieldFormatter {

		private final DateTimeFormatter delegate;

		protected LocalDateFieldFormatter(DateTimeFormatter delegate) {
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
			LocalDateFieldFormatter other = (LocalDateFieldFormatter) obj;
			return delegate.equals( other.delegate );
		}

		@Override
		public int hashCode() {
			return delegate.hashCode();
		}

	}
}
