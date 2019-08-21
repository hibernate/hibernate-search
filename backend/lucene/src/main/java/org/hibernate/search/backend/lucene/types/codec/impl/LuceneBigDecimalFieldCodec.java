/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneLongDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.cfg.spi.NumberScaleConstants;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneBigDecimalFieldCodec extends AbstractLuceneNumericFieldCodec<BigDecimal, Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final int decimalScale;
	private final BigDecimal minScaledValue;
	private final BigDecimal maxScaledValue;

	public LuceneBigDecimalFieldCodec(boolean projectable, boolean searchable, boolean sortable, BigDecimal indexNullAsValue, int decimalScale) {
		super( projectable, searchable, sortable, indexNullAsValue );
		this.decimalScale = decimalScale;
		this.minScaledValue = new BigDecimal( NumberScaleConstants.MIN_LONG_AS_BIGINTEGER, decimalScale );
		this.maxScaledValue = new BigDecimal( NumberScaleConstants.MAX_LONG_AS_BIGINTEGER, decimalScale );
	}

	@Override
	void doEncodeForProjection(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, BigDecimal value,
			Long encodedValue) {
		// storing field as String for projections
		documentBuilder.addField( new StoredField( absoluteFieldPath, value.toString() ) );
	}

	@Override
	public BigDecimal decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );
		if ( field == null ) {
			return null;
		}

		return new BigDecimal( field.stringValue() );
	}

	@Override
	public Long encode(BigDecimal value) {
		if ( isTooLarge( value ) ) {
			throw log.scaledNumberTooLarge( value );
		}

		return unscale( value );
	}

	@Override
	public LuceneNumericDomain<Long> getDomain() {
		return LuceneLongDomain.get();
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.isCompatibleWith( obj ) ) {
			return false;
		}

		LuceneBigDecimalFieldCodec other = (LuceneBigDecimalFieldCodec) obj;
		return decimalScale == other.decimalScale;
	}

	private Long unscale(BigDecimal value) {
		// See tck.DecimalScaleIT#roundingMode
		return value.setScale( decimalScale, RoundingMode.HALF_UP ).unscaledValue().longValue();
	}

	private boolean isTooLarge(BigDecimal value) {
		return ( value.compareTo( minScaledValue ) < 0 || value.compareTo( maxScaledValue ) > 0 );
	}
}
