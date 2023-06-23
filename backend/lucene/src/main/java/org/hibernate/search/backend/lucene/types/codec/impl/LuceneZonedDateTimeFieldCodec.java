/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;

import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneLongDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.util.common.impl.TimeHelper;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneZonedDateTimeFieldCodec extends AbstractLuceneNumericFieldCodec<ZonedDateTime, Long> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( LuceneOffsetDateTimeFieldCodec.FORMATTER )
			// ZoneRegionId is optional
			.optionalStart()
			.appendLiteral( '[' )
			.parseCaseSensitive()
			.appendZoneRegionId()
			.appendLiteral( ']' )
			.optionalEnd()
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	public LuceneZonedDateTimeFieldCodec(Indexing indexing, DocValues docValues, Storage storage,
			ZonedDateTime indexNullAsValue) {
		super( indexing, docValues, storage, indexNullAsValue );
	}

	@Override
	void addStoredToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, ZonedDateTime value,
			Long encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, FORMATTER.format( value ) ) );
	}

	@Override
	public ZonedDateTime decode(IndexableField field) {
		String value = field.stringValue();

		if ( value == null ) {
			return null;
		}

		return TimeHelper.parseZoneDateTime( value, FORMATTER );
	}

	@Override
	public Long encode(ZonedDateTime value) {
		return value == null ? null : value.toInstant().toEpochMilli();
	}

	@Override
	public ZonedDateTime decode(Long encoded) {
		return Instant.ofEpochMilli( encoded ).atZone( ZoneOffset.UTC );
	}

	@Override
	public LuceneNumericDomain<Long> getDomain() {
		return LuceneLongDomain.get();
	}
}
