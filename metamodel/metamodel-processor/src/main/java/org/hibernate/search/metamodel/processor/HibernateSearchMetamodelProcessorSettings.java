/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor;


import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import javax.lang.model.util.Elements;

public final class HibernateSearchMetamodelProcessorSettings {

	private HibernateSearchMetamodelProcessorSettings() {
	}

	private static final String PREFIX = "org.hibernate.search.metamodel.processor.";

	public static final String ADD_GENERATED_ANNOTATION = PREFIX + Radicals.ADD_GENERATED_ANNOTATION;
	public static final String BACKEND_VERSION = PREFIX + Radicals.BACKEND_VERSION;

	public static class Radicals {

		private Radicals() {
		}

		public static final String ADD_GENERATED_ANNOTATION = "add_generated_annotation";
		public static final String BACKEND_VERSION = "backend.version";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		public static final String ADD_GENERATED_ANNOTATION = Boolean.TRUE.toString();

		private Defaults() {
		}
	}

	public record Configuration(boolean addGeneratedAnnotation, String version) implements Serializable {
		public Configuration(Map<String, String> options) {
			this(
					Boolean.parseBoolean( options.getOrDefault( ADD_GENERATED_ANNOTATION, Defaults.ADD_GENERATED_ANNOTATION ) ),
					Objects.toString( options.get( BACKEND_VERSION ), null )
			);
		}

		public String elasticsearchVersion() {
			return version == null ? "9.0.0" : version;
		}

		public String luceneVersion() {
			return version == null ? "9.12.1" : version;
		}

		public String backendVersion() {
			return isLuceneBackend() ? luceneVersion() : elasticsearchVersion();
		}

		public boolean isOrmMapperPresent(Elements elementUtils) {
			return elementUtils.getTypeElement( "org.hibernate.search.mapper.orm.Search" ) != null;
		}

		private boolean isLuceneBackend() {
			try {
				Class.forName( "org.hibernate.search.backend.lucene.LuceneBackend" );
			}
			catch (ClassNotFoundException e) {
				return false;
			}
			return true;
		}
	}
}
