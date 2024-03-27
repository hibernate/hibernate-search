/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded;

/**
 * @author Emmanuel Bernard
 */
public interface Person {

	String getName();

	void setName(String name);

	Address getAddress();

	void setAddress(Address address);

}
