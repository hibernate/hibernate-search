/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Normalizer;

@Entity
@Indexed(index = "idx1")
@Analyzer(impl = AnalyzerForTests1.class)
public class AlarmEntity {

	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;

	@Field
	private String property;

	@Fields({
			@Field(name = "description_analyzer2", analyzer = @Analyzer(impl = AnalyzerForTests2.class)),
			@Field(name = "description_analyzer3", analyzer = @Analyzer(impl = AnalyzerForTests3.class)),
			@Field(name = "description_normalizer1", normalizer = @Normalizer(impl = NormalizerForTests1.class))
	})
	private String alarmDescription;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public String getAlarmDescription() {
		return alarmDescription;
	}

	public void setAlarmDescription(String alarmDescription) {
		this.alarmDescription = alarmDescription;
	}

}
