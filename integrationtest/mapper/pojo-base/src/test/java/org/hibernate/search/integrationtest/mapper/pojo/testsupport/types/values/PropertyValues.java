/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PropertyValues<EV, DV> {

	public final List<EV> entityModelValues;
	public final List<DV> documentFieldValues;
	public final List<String> documentIdentifierValues;

	public PropertyValues(List<SingleValue<EV, DV>> tuples) {
		this.entityModelValues = Collections.unmodifiableList( tuples.stream()
				.map( v -> v.entityModelValue ).collect( Collectors.toList() ) );
		this.documentFieldValues = Collections.unmodifiableList( tuples.stream()
				.map( v -> v.documentFieldValue ).collect( Collectors.toList() ) );
		this.documentIdentifierValues = Collections.unmodifiableList( tuples.stream()
				.map( v -> v.documentIdentifierValue ).collect( Collectors.toList() ) );
	}

	public static <EV, DV> Builder<EV, DV> builder() {
		return new Builder<>();
	}

	public static <EV> StringBasedBuilder<EV> stringBasedBuilder() {
		return new StringBasedBuilder<>();
	}

	public static <EV> PassThroughBuilder<EV> passThroughBuilder() {
		return new PassThroughBuilder<>();
	}

	public static final class Builder<EV, DV> {
		private final List<SingleValue<EV, DV>> tuples = new ArrayList<>();

		private Builder() {
		}

		public Builder<EV, DV> add(EV entityModelValue, DV documentFieldValue, String documentIdentifierValue) {
			tuples.add( new SingleValue<>( entityModelValue, documentFieldValue, documentIdentifierValue ) );
			return this;
		}

		// TODO HSEARCH-4331 Remove this and use version with identifier everywhere
		public Builder<EV, DV> add(EV entityModelValue, DV documentFieldValue) {
			return add( entityModelValue, documentFieldValue, null );
		}

		public PropertyValues<EV, DV> build() {
			return new PropertyValues<>( tuples );
		}
	}

	public static final class PassThroughBuilder<EV> {
		private final List<SingleValue<EV, EV>> tuples = new ArrayList<>();

		private PassThroughBuilder() {
		}

		public PassThroughBuilder<EV> add(EV entityModelValue, String documentIdentifierValue) {
			tuples.add( new SingleValue<>( entityModelValue, entityModelValue, documentIdentifierValue ) );
			return this;
		}

		// TODO HSEARCH-4331 Remove this and use version with identifier everywhere
		public PassThroughBuilder<EV> add(EV entityModelValue) {
			return add( entityModelValue, null );
		}

		public PropertyValues<EV, EV> build() {
			return new PropertyValues<>( tuples );
		}
	}

	public static final class StringBasedBuilder<EV> {
		private final List<SingleValue<EV, String>> tuples = new ArrayList<>();

		private StringBasedBuilder() {
		}

		public StringBasedBuilder<EV> add(EV entityModelValue, String documentFieldOrIdentifierValue) {
			tuples.add( new SingleValue<>( entityModelValue, documentFieldOrIdentifierValue, documentFieldOrIdentifierValue ) );
			return this;
		}

		public PropertyValues<EV, String> build() {
			return new PropertyValues<>( tuples );
		}
	}

	private static final class SingleValue<EV, DV> {
		public final EV entityModelValue;
		public final DV documentFieldValue;
		public final String documentIdentifierValue;

		private SingleValue(EV entityModelValue, DV documentFieldValue, String documentIdentifierValue) {
			this.entityModelValue = entityModelValue;
			this.documentFieldValue = documentFieldValue;
			this.documentIdentifierValue = documentIdentifierValue;
		}
	}
}
