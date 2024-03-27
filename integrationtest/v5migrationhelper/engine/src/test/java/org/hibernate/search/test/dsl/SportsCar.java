/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.dsl;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Gunnar Morling
 */
@Indexed
class SportsCar extends Car {

	@Field(analyze = Analyze.NO)
	private int enginePower;

	public SportsCar(Integer id, String name, int enginePower) {
		super( id, name );
		this.enginePower = enginePower;
	}

	public int getEnginePower() {
		return enginePower;
	}

	public void setEnginePower(int enginePower) {
		this.enginePower = enginePower;
	}
}
