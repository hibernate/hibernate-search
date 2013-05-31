/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.engine.metadata.impl;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.DefaultBoostStrategy;
import org.hibernate.search.util.impl.ReflectionHelper;

/**
 * Encapsulating the metadata for a single indexed property annotated with {@code Field}.
 *
 * @author Hardy Ferentschik
 */
public class PropertyMetadata {
	private final XProperty propertyGetter;
	private final DocumentFieldMetadata fieldMetadata;
	private final BoostStrategy dynamicBoostStrategy;
	private final Integer precisionStep;
	private final String nullToken;

	private PropertyMetadata(Builder builder) {
		this.propertyGetter = builder.propertyGetter;
		this.fieldMetadata = builder.fieldMetadata;
		this.nullToken = builder.nullToken;

		if ( builder.dynamicBoostStrategy != null ) {
			this.dynamicBoostStrategy = builder.dynamicBoostStrategy;
		}
		else {
			this.dynamicBoostStrategy = DefaultBoostStrategy.INSTANCE;
		}

		if ( builder.precisionStep == null ) {
			this.precisionStep = NumericField.PRECISION_STEP_DEFAULT;
		}
		else {
			this.precisionStep = builder.precisionStep;
		}
	}

	public XProperty getPropertyGetter() {
		return propertyGetter;
	}

	public String getPropertyGetterName() {
		return propertyGetter == null ? null : propertyGetter.getName();
	}

	public Integer getPrecisionStep() {
		return precisionStep;
	}

	public String getNullToken() {
		return nullToken;
	}

	public DocumentFieldMetadata getFieldMetadata() {
		return fieldMetadata;
	}

	public BoostStrategy getDynamicBoostStrategy() {
		return dynamicBoostStrategy;
	}

	public static class Builder {
		// required parameters
		private final XProperty propertyGetter;
		private final DocumentFieldMetadata fieldMetadata;

		// optional parameters
		private BoostStrategy dynamicBoostStrategy;
		private Integer precisionStep;
		private String nullToken;

		public Builder(XProperty propertyGetter, DocumentFieldMetadata fieldMetadata) {
			if ( propertyGetter != null ) {
				ReflectionHelper.setAccessible( propertyGetter );
			}
			this.propertyGetter = propertyGetter;
			this.fieldMetadata = fieldMetadata;
		}

		public Builder dynamicBoostStrategy(BoostStrategy boostStrategy) {
			this.dynamicBoostStrategy = boostStrategy;
			return this;
		}

		public Builder precisionStep(Integer precisionStep) {
			this.precisionStep = precisionStep;
			return this;
		}

		public Builder indexNullAs(String nullToken) {
			this.nullToken = nullToken;
			return this;
		}

		public PropertyMetadata build() {
			return new PropertyMetadata( this );
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "PropertyMetadata{" );
		sb.append( "propertyGetter=" ).append( propertyGetter );
		sb.append( ", fieldMetadata=" ).append( fieldMetadata );
		sb.append( ", dynamicBoostStrategy=" ).append( dynamicBoostStrategy );
		sb.append( ", precisionStep=" ).append( precisionStep );
		sb.append( ", nullToken='" ).append( nullToken ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}


