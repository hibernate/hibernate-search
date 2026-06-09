/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.mapping.impl;

import java.lang.annotation.Annotation;

import jakarta.persistence.AccessType;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.EnumType;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.id.uuid.UuidValueGenerator;

/**
 * Simplified annotation implementations for programmatic entity mapping.
 * <p>
 * These replace the internal ORM implementations
 * ({@code org.hibernate.boot.models.annotations.internal.*})
 * which are not accessible from external JPMS modules.
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
class AdditionalMappingAnnotations {

	static class EntityAnnotation implements jakarta.persistence.Entity {
		private String name = "";

		@Override
		public Class<? extends Annotation> annotationType() {
			return jakarta.persistence.Entity.class;
		}

		@Override
		public String name() {
			return name;
		}

		public void name(String value) {
			this.name = value;
		}
	}

	static class TableAnnotation implements jakarta.persistence.Table {
		private String name = "";
		private String catalog = "";
		private String schema = "";
		private UniqueConstraint[] uniqueConstraints = { };
		private jakarta.persistence.Index[] indexes = { };
		private CheckConstraint[] check = { };
		private String comment = "";
		private String type = "";
		private String options = "";

		@Override
		public Class<? extends Annotation> annotationType() {
			return jakarta.persistence.Table.class;
		}

		@Override
		public String name() {
			return name;
		}

		public void name(String value) {
			this.name = value;
		}

		@Override
		public String catalog() {
			return catalog;
		}

		public void catalog(String value) {
			this.catalog = value;
		}

		@Override
		public String schema() {
			return schema;
		}

		public void schema(String value) {
			this.schema = value;
		}

		@Override
		public UniqueConstraint[] uniqueConstraints() {
			return uniqueConstraints;
		}

		@Override
		public jakarta.persistence.Index[] indexes() {
			return indexes;
		}

		public void indexes(jakarta.persistence.Index[] value) {
			this.indexes = value;
		}

		@Override
		public CheckConstraint[] check() {
			return check;
		}

		@Override
		public String comment() {
			return comment;
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public String options() {
			return options;
		}
	}

	static class IndexAnnotation implements jakarta.persistence.Index {
		private String name = "";
		private String columnList = "";
		private boolean unique = false;
		private String type = "";
		private String using = "";
		private String options = "";

		@Override
		public Class<? extends Annotation> annotationType() {
			return jakarta.persistence.Index.class;
		}

		@Override
		public String name() {
			return name;
		}

		public void name(String value) {
			this.name = value;
		}

		@Override
		public String columnList() {
			return columnList;
		}

		public void columnList(String value) {
			this.columnList = value;
		}

		@Override
		public boolean unique() {
			return unique;
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public String using() {
			return using;
		}

		@Override
		public String options() {
			return options;
		}
	}

	static class ColumnAnnotation implements jakarta.persistence.Column {
		private String name = "";
		private boolean unique = false;
		private boolean nullable = true;
		private boolean insertable = true;
		private boolean updatable = true;
		private String columnDefinition = "";
		private String options = "";
		private String table = "";
		private int length = 255;
		private int precision = 0;
		private int scale = 0;
		private int secondPrecision = -1;
		private CheckConstraint[] check = { };
		private String comment = "";

		@Override
		public Class<? extends Annotation> annotationType() {
			return jakarta.persistence.Column.class;
		}

		@Override
		public String name() {
			return name;
		}

		public void name(String value) {
			this.name = value;
		}

		@Override
		public boolean unique() {
			return unique;
		}

		@Override
		public boolean nullable() {
			return nullable;
		}

		public void nullable(boolean value) {
			this.nullable = value;
		}

		@Override
		public boolean insertable() {
			return insertable;
		}

		@Override
		public boolean updatable() {
			return updatable;
		}

		@Override
		public String columnDefinition() {
			return columnDefinition;
		}

		@Override
		public String options() {
			return options;
		}

		@Override
		public String table() {
			return table;
		}

		@Override
		public int length() {
			return length;
		}

		public void length(int value) {
			this.length = value;
		}

		@Override
		public int precision() {
			return precision;
		}

		@Override
		public int scale() {
			return scale;
		}

		@Override
		public int secondPrecision() {
			return secondPrecision;
		}

		@Override
		public CheckConstraint[] check() {
			return check;
		}

		@Override
		public String comment() {
			return comment;
		}
	}

	static class AccessAnnotation implements jakarta.persistence.Access {
		private AccessType value;

		@Override
		public Class<? extends Annotation> annotationType() {
			return jakarta.persistence.Access.class;
		}

		@Override
		public AccessType value() {
			return value;
		}

		public void value(AccessType value) {
			this.value = value;
		}
	}

	static class EnumeratedAnnotation implements jakarta.persistence.Enumerated {
		private EnumType value = EnumType.ORDINAL;

		@Override
		public Class<? extends Annotation> annotationType() {
			return jakarta.persistence.Enumerated.class;
		}

		@Override
		public EnumType value() {
			return value;
		}

		public void value(EnumType value) {
			this.value = value;
		}
	}

	static class IdAnnotation implements jakarta.persistence.Id {
		@Override
		public Class<? extends Annotation> annotationType() {
			return jakarta.persistence.Id.class;
		}
	}

	static class TenantIdAnnotation implements org.hibernate.annotations.TenantId {
		@Override
		public Class<? extends Annotation> annotationType() {
			return org.hibernate.annotations.TenantId.class;
		}
	}

	static class UuidGeneratorAnnotation implements UuidGenerator {
		private Style style = Style.AUTO;
		private Class<? extends UuidValueGenerator> algorithm = UuidValueGenerator.class;

		@Override
		public Class<? extends Annotation> annotationType() {
			return UuidGenerator.class;
		}

		@Override
		public Style style() {
			return style;
		}

		public void style(Style value) {
			this.style = value;
		}

		@Override
		public Class<? extends UuidValueGenerator> algorithm() {
			return algorithm;
		}
	}

	static class JdbcTypeCodeAnnotation implements org.hibernate.annotations.JdbcTypeCode {
		private int value;

		@Override
		public Class<? extends Annotation> annotationType() {
			return org.hibernate.annotations.JdbcTypeCode.class;
		}

		@Override
		public int value() {
			return value;
		}

		public void value(int value) {
			this.value = value;
		}
	}

	private AdditionalMappingAnnotations() {
	}
}
