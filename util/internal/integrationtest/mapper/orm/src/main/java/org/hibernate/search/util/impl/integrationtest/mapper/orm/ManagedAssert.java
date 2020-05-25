/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	public ManagedAssert isInitialized(boolean expectInitialized) {
		isNotNull();
		managedInitialization().isEqualTo( expectInitialized );
		return this;
	}

	public ManagedAssert isInitialized() {
		return isInitialized( true );
	}

	public ManagedAssert isNotInitialized() {
		return isInitialized( false );
	}

	public ManagedAssert hasPropertyInitialized(String propertyName, boolean expectInitialized) {
		isNotNull();
		propertyInitialization( propertyName ).isEqualTo( expectInitialized );
		return this;
	}

	public ManagedAssert hasPropertyInitialized(String propertyName) {
		return hasPropertyInitialized( propertyName, true );
	}

	public ManagedAssert hasPropertyNotInitialized(String propertyName) {
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
