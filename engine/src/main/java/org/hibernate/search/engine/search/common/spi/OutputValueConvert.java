/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common.spi;

import org.hibernate.search.engine.search.common.AggregationOutputValueConvert;
import org.hibernate.search.engine.search.common.ProjectionValueConvert;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;

public enum OutputValueConvert {
	MAPPING,
	INDEX,
	RAW;

	public static OutputValueConvert from(AggregationOutputValueConvert valueConvert) {
		Contracts.assertNotNull( valueConvert, "valueConvert" );
		switch ( valueConvert ) {
			case MAPPING:
				return MAPPING;
			case INDEX:
				return INDEX;
			case RAW:
				return RAW;
			default:
				throw new AssertionFailure( "Unsupported SortValueConvert: " + valueConvert );
		}
	}

	public static OutputValueConvert from(ProjectionValueConvert valueConvert) {
		Contracts.assertNotNull( valueConvert, "valueConvert" );
		switch ( valueConvert ) {
			case MAPPING:
				return MAPPING;
			case INDEX:
				return INDEX;
			default:
				throw new AssertionFailure( "Unsupported SortValueConvert: " + valueConvert );
		}
	}
}
