/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common.spi;

import org.hibernate.search.engine.search.common.AggregationInputValueConvert;
import org.hibernate.search.engine.search.common.PredicateValueConvert;
import org.hibernate.search.engine.search.common.SortValueConvert;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;

public enum InputValueConvert {
	MAPPING,
	INDEX,
	STRING;

	public static InputValueConvert from(SortValueConvert valueConvert) {
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

	public static InputValueConvert from(PredicateValueConvert valueConvert) {
		Contracts.assertNotNull( valueConvert, "valueConvert" );
		switch ( valueConvert ) {
			case MAPPING:
				return MAPPING;
			case INDEX:
				return INDEX;
			case STRING:
				return STRING;
			default:
				throw new AssertionFailure( "Unsupported SortValueConvert: " + valueConvert );
		}
	}

	public static InputValueConvert from(AggregationInputValueConvert valueConvert) {
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
