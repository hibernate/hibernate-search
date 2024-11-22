/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.impl;

import java.util.Collection;

import org.hibernate.search.util.common.logging.impl.CommonMiscLog;

public final class Contracts {

	private Contracts() {
	}

	public static void assertNotNull(Object object, String objectDescription) {
		if ( object == null ) {
			throw CommonMiscLog.INSTANCE.mustNotBeNull( objectDescription );
		}
	}

	public static void assertNotNullNorEmpty(Collection<?> object, String objectDescription) {
		if ( object == null || object.isEmpty() ) {
			throw CommonMiscLog.INSTANCE.collectionMustNotBeNullNorEmpty( objectDescription );
		}
	}

	public static void assertNotNullNorEmpty(Object[] object, String objectDescription) {
		if ( object == null || object.length == 0 ) {
			throw CommonMiscLog.INSTANCE.arrayMustNotBeNullNorEmpty( objectDescription );
		}
	}

	public static void assertPositiveOrZero(int number, String objectDescription) {
		if ( number < 0 ) {
			throw CommonMiscLog.INSTANCE.mustBePositiveOrZero( objectDescription );
		}
	}

	public static void assertStrictlyPositive(int number, String objectDescription) {
		if ( number <= 0 ) {
			throw CommonMiscLog.INSTANCE.mustBeStrictlyPositive( objectDescription );
		}
	}

	public static void assertStrictlyPositive(long number, String objectDescription) {
		if ( number <= 0 ) {
			throw CommonMiscLog.INSTANCE.mustBePositiveOrZero( objectDescription );
		}
	}

	public static void assertNotNullNorEmpty(String object, String objectDescription) {
		if ( object == null || object.isEmpty() ) {
			throw CommonMiscLog.INSTANCE.stringMustNotBeNullNorEmpty( objectDescription );
		}
	}

	public static void assertNoNullElement(Collection<?> collection, String collectionDescription) {
		if ( collection != null && collection.contains( null ) ) {
			throw CommonMiscLog.INSTANCE.collectionMustNotContainNullElement( collectionDescription );
		}
	}

}
