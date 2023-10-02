/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneLongDomain;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.cfg.spi.NumberScaleConstants;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneBigDecimalFieldCodec extends AbstractLuceneNumericFieldCodec<BigDecimal, Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final int decimalScale;
	private final BigDecimal minScaledValue;
	private final BigDecimal maxScaledValue;

	public LuceneBigDecimalFieldCodec(Indexing indexing, DocValues docValues, Storage storage,
			BigDecimal indexNullAsValue, int decimalScale) {
		super( indexing, docValues, storage, indexNullAsValue );
		this.decimalScale = decimalScale;
		this.minScaledValue = new BigDecimal( NumberScaleConstants.MIN_LONG_AS_BIGINTEGER, decimalScale );
		this.maxScaledValue = new BigDecimal( NumberScaleConstants.MAX_LONG_AS_BIGINTEGER, decimalScale );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "decimalScale=" + decimalScale
				+ "]";
	}

	@Override
	void addStoredToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, BigDecimal value,
			Long encodedValue) {
		documentBuilder.addField( new StoredField( absoluteFieldPath, value.toString() ) );
	}

	@Override
	public BigDecimal decode(IndexableField field) {
		return new BigDecimal( field.stringValue() );
	}

	@Override
	public Long encode(BigDecimal value) {
		if ( value.compareTo( minScaledValue ) < 0 || value.compareTo( maxScaledValue ) > 0 ) {
			throw log.scaledNumberTooLarge( value, minScaledValue, maxScaledValue );
		}

		return unscale( value );
	}

	@Override
	public BigDecimal decode(Long encoded) {
		return scale( encoded );
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

	private BigDecimal scale(Long value) {
		return new BigDecimal( BigInteger.valueOf( value ), decimalScale );
	}

}
