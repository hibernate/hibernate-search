/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor;


import java.io.Serializable;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.lang.model.util.Elements;

public final class HibernateSearchProcessorSettings {

	private HibernateSearchProcessorSettings() {
	}

	private static final String PREFIX = "org.hibernate.search.processor.";

	public static final String GENERATED_ANNOTATION_ADD = PREFIX + Radicals.GENERATED_ANNOTATION_ADD;
	public static final String GENERATED_ANNOTATION_TIMESTAMP = PREFIX + Radicals.GENERATED_ANNOTATION_TIMESTAMP;
	public static final String BACKEND_VERSION = PREFIX + Radicals.BACKEND_VERSION;

	public static class Radicals {

		private Radicals() {
		}

		public static final String GENERATED_ANNOTATION_ADD = "generated_annotation.add";
		public static final String GENERATED_ANNOTATION_TIMESTAMP = "generated_annotation.timestamp";
		public static final String BACKEND_VERSION = "backend.version";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		public static final String GENERATED_ANNOTATION_ADD = Boolean.TRUE.toString();
		public static final String GENERATED_ANNOTATION_TIMESTAMP = Boolean.FALSE.toString();

		private Defaults() {
		}
	}

	public record Configuration(boolean generatedAnnotationAdd, boolean generatedAnnotationTimestamp, String version)
			implements Serializable {
		public Configuration(Map<String, String> options) {
			this(
					Boolean.parseBoolean( options.getOrDefault( GENERATED_ANNOTATION_ADD, Defaults.GENERATED_ANNOTATION_ADD ) ),
					Boolean.parseBoolean(
							options.getOrDefault( GENERATED_ANNOTATION_TIMESTAMP, Defaults.GENERATED_ANNOTATION_TIMESTAMP ) ),
					Objects.toString( options.get( BACKEND_VERSION ), null )
			);
		}

		public String formattedGeneratedAnnotation() {
			if ( !generatedAnnotationAdd ) {
				return "";
			}
			if ( generatedAnnotationTimestamp ) {
				return String.format(
						Locale.ROOT, "@javax.annotation.processing.Generated(value = \"%s\", date = \"%s\")",
						HibernateSearchProcessor.class.getName(),
						LocalDateTime.now( Clock.systemUTC() ).atOffset( ZoneOffset.UTC )
								.format( DateTimeFormatter.ISO_OFFSET_DATE_TIME )
				);
			}
			else {
				return String.format( Locale.ROOT, "@javax.annotation.processing.Generated(value = \"%s\")",
						HibernateSearchProcessor.class.getName()
				);
			}
		}

		public String elasticsearchVersion() {
			return version == null ? "9.0.1" : version;
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
