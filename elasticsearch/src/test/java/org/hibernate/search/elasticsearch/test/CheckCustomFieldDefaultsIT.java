/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.test.bridge.CheckCustomFieldDefaultAnalyzer;
import org.hibernate.search.test.bridge.CheckCustomFieldDefaultsTest;

/**
 * Test that custom fields created using a {@link MetadataProvidingFieldBridge} use the expected
 * defaults.
 *
 * @see CheckCustomFieldDefaultsTest
 * @author Davide D'Alto
 */
public class CheckCustomFieldDefaultsIT extends CheckCustomFieldDefaultAnalyzer {

	@Override
	protected Object entity(String id, String code, String result) {
		Experiment experiment = new Experiment();
		experiment.id = id;
		experiment.subject = code;
		experiment.result = result;
		return experiment;
	}


	@Override
	protected Class<?> getEntityType() {
		return Experiment.class;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Experiment.class };
	}

	@Entity
	@Indexed
	public static class Experiment {

		@Id
		@FieldBridge(impl = AdditionalFieldBridge.class)
		public String id;

		@Field(analyze = Analyze.NO)
		@FieldBridge(impl = AdditionalFieldBridge.class)
		public String subject;

		@Field(analyze = Analyze.YES)
		@FieldBridge(impl = AdditionalFieldBridge.class)
		@Analyzer(definition = "whitespace")
		public String result;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
			result = prime * result + ( ( this.result == null ) ? 0 : this.result.hashCode() );
			result = prime * result + ( ( subject == null ) ? 0 : subject.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			Experiment other = (Experiment) obj;
			if ( id == null ) {
				if ( other.id != null ) {
					return false;
				}
			}
			else if ( !id.equals( other.id ) ) {
				return false;
			}
			if ( result == null ) {
				if ( other.result != null ) {
					return false;
				}
			}
			else if ( !result.equals( other.result ) ) {
				return false;
			}
			if ( subject == null ) {
				if ( other.subject != null ) {
					return false;
				}
			}
			else if ( !subject.equals( other.subject ) ) {
				return false;
			}
			return true;
		}
	}
}
