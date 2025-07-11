/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.serialization.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;

import org.junit.jupiter.api.Test;

class SerializationUtilsTest {

	@SuppressWarnings("removal")
	@Test
	void smoke() {
		MyRecord test = new MyRecord( 1, "smth" );
		byte[] serialized = SerializationUtils.serialize( test );

		MyRecord deserialized = SerializationUtils.deserialize( MyRecord.class, serialized );
		assertThat( deserialized ).isEqualTo( test );
	}

	record MyRecord(int number, String string) implements Serializable {
	}
}
