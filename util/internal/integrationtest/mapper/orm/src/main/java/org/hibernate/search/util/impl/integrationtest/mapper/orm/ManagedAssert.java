/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Hibernate;

import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractObjectAssert;

public class ManagedAssert<T> extends AbstractObjectAssert<ManagedAssert<T>, T> {

	public static <T> ManagedAssert<T> assertThatManaged(T entity) {
		return new ManagedAssert<>( entity );
	}

	private ManagedAssert(T t) {
		super( t, ManagedAssert.class );
	}

	public ManagedAssert<T> isInitialized(boolean expectInitialized) {
		isNotNull();
		managedInitialization().isEqualTo( expectInitialized );
		return this;
	}

	public ManagedAssert<T> isInitialized() {
		return isInitialized( true );
	}

	public ManagedAssert<T> isNotInitialized() {
		return isInitialized( false );
	}

	public ManagedAssert<T> hasPropertyInitialized(String propertyName, boolean expectInitialized) {
		isNotNull();
		propertyInitialization( propertyName ).isEqualTo( expectInitialized );
		return this;
	}

	public ManagedAssert<T> hasPropertyInitialized(String propertyName) {
		return hasPropertyInitialized( propertyName, true );
	}

	public ManagedAssert<T> hasPropertyNotInitialized(String propertyName) {
		return hasPropertyInitialized( propertyName, false );
	}

	private AbstractBooleanAssert<?> managedInitialization() {
		return assertThat( Hibernate.isInitialized( actual ) )
				.as( "Is '" + actualAsText() + "' initialized?" );
	}

	private AbstractBooleanAssert<?> propertyInitialization(String propertyName) {
		return assertThat( Hibernate.isPropertyInitialized( actual, propertyName ) )
				.as( "Is property '" + propertyName + "' of '" + actualAsText() + "' initialized?" );
	}

	private String actualAsText() {
		String text = descriptionText();
		if ( text == null || text.isEmpty() ) {
			text = String.valueOf( actual );
		}
		return text;
	}

}
