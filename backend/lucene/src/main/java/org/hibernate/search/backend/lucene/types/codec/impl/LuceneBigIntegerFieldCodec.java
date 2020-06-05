/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneLongDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.cfg.spi.NumberScaleConstants;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneBigIntegerFieldCodec extends AbstractLuceneNumericFieldCodec<BigInteger, Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final int decimalScale;
	private final BigDecimal minScaledValue;
	private final BigDecimal maxScaledValue;

	public LuceneBigIntegerFieldCodec(boolean projectable, boolean searchable, boolean sortable,
			boolean aggregable, BigInteger indexNullAsValue, int decimalScale) {
		super( projectable, searchable, sortable, aggregable, indexNullAsValue );
		this.decimalScale = decimalScale;
		this.minScaledValue = new BigDecimal( NumberScaleConstants.MIN_LONG_AS_BIGINTEGER, decimalScale );
		this.maxScaledValue = new BigDecimal( NumberScaleConstants.MAX_LONG_AS_BIGINTEGER, decimalScale );
	}

	@Override
	void doEncodeForProjection(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, BigInteger value,
			Long encodedValue) {
		// storing field as String for projections
		documentBuilder.addField( new StoredField( absoluteFieldPath, value.toString() ) );
	}

	@Override
	public BigInteger decode(IndexableField field) {
		return new BigInteger( field.stringValue() );
	}

	@Override
	public Long encode(BigInteger value) {
		BigDecimal decimal = new BigDecimal( value );
		if ( isTooLarge( decimal ) ) {
			throw log.scaledNumberTooLarge( value );
		}

		return unscale( decimal );
	}

	@Override
	public BigInteger decode(Long encoded) {
		return scale( encoded ).toBigInteger();
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

		LuceneBigIntegerFieldCodec other = (LuceneBigIntegerFieldCodec) obj;
		return decimalScale == other.decimalScale;
	}

	private Long unscale(BigDecimal decimal) {
		// See tck.DecimalScaleIT#roundingMode
		return decimal.setScale( decimalScale, RoundingMode.HALF_UP ).unscaledValue().longValue();
	}

	private BigDecimal scale(Long value) {
		return new BigDecimal( BigInteger.valueOf( value ), decimalScale );
	}

	private boolean isTooLarge(BigDecimal decimal) {
		return ( decimal.compareTo( minScaledValue ) < 0 || decimal.compareTo( maxScaledValue ) > 0 );
	}
}
