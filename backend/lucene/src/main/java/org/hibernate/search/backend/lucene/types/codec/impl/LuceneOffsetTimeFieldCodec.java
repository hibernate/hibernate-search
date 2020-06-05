/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.time.Instant;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneLongDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneOffsetTimeFieldCodec extends AbstractLuceneNumericFieldCodec<OffsetTime, Long> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
			.append( LuceneLocalTimeFieldCodec.FORMATTER )
			// OffsetId is mandatory
			.appendOffsetId()
			.toFormatter( Locale.ROOT )
			.withResolverStyle( ResolverStyle.STRICT );

	public LuceneOffsetTimeFieldCodec(boolean projectable, boolean searchable, boolean sortable,
			boolean aggregable, OffsetTime indexNullAsValue) {
		super( projectable, searchable, sortable, aggregable, indexNullAsValue );
	}

	@Override
	void doEncodeForProjection(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, OffsetTime value,
			Long encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, FORMATTER.format( value ) ) );
	}

	@Override
	public OffsetTime decode(IndexableField field) {
		String value = field.stringValue();

		if ( value == null ) {
			return null;
		}

		return OffsetTime.parse( value, FORMATTER );
	}

	@Override
	public Long encode(OffsetTime value) {
		if ( value == null ) {
			return null;
		}

		// see private method OffsetTime#toEpochNano:
		long nod = value.toLocalTime().toNanoOfDay();
		long offsetNanos = value.getOffset().getTotalSeconds() * 1_000_000_000L;
		return nod - offsetNanos;
	}

	@Override
	public OffsetTime decode(Long encoded) {
		// See encode; we just add the nanos to EPOCH, and get the corresponding UTC time
		return Instant.EPOCH.plus( encoded, ChronoUnit.NANOS )
				.atOffset( ZoneOffset.UTC ).toOffsetTime();
	}

	@Override
	public LuceneNumericDomain<Long> getDomain() {
		return LuceneLongDomain.get();
	}
}
