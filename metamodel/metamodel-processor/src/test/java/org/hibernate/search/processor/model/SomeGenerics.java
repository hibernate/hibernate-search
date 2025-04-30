/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.model;

import java.util.List;
import java.util.Map;

public interface SomeGenerics {

	interface MyList<K> extends List<K> {}

	interface MyStringList extends MyList<String> {}

	interface MyMap<A, B> extends Map<A, B> {}

	interface MyStringKeyMap<H> extends MyMap<String, H> {}

	interface MyStringStringMap extends MyStringKeyMap<String> {}

}
