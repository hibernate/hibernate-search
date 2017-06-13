/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test.filter;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.FilterCacheModeType;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.hibernate.search.test.filter.FieldConstraintFilterFactory;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
@FullTextFilterDef(name = "bestDriver",
		impl = BestDriversElasticsearchFilter.class,
		cache = FilterCacheModeType.NONE)
@FullTextFilterDef(name = "namedDriver",
		impl = DriversMatchingNameElasticsearchFilter.class)
@FullTextFilterDef(name = "fieldConstraintFilter-1",
		impl = FieldConstraintFilterFactory.class,
		cache = FilterCacheModeType.INSTANCE_AND_DOCIDSETRESULTS)
public class Driver {
	@Id
	@DocumentId
	private int id;

	@Field(analyze = Analyze.YES)
	private String name;

	@Field(analyze = Analyze.NO)
	private String teacher;

	@Field(analyze = Analyze.NO)
	@FieldBridge(impl = IntegerBridge.class)
	private int score;

	@Field(analyze = Analyze.NO)
	@DateBridge(resolution = Resolution.YEAR)
	private Date delivery;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTeacher() {
		return teacher;
	}

	public void setTeacher(String teacher) {
		this.teacher = teacher;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public Date getDelivery() {
		return delivery;
	}

	public void setDelivery(Date delivery) {
		this.delivery = delivery;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Driver driver = (Driver) o;

		if ( id != driver.id ) {
			return false;
		}
		if ( score != driver.score ) {
			return false;
		}
		if ( delivery != null ? !delivery.equals( driver.delivery ) : driver.delivery != null ) {
			return false;
		}
		if ( name != null ? !name.equals( driver.name ) : driver.name != null ) {
			return false;
		}
		return !( teacher != null ? !teacher.equals( driver.teacher ) : driver.teacher != null );

	}

	@Override
	public int hashCode() {
		int result;
		result = id;
		result = 31 * result + ( name != null ? name.hashCode() : 0 );
		result = 31 * result + ( teacher != null ? teacher.hashCode() : 0 );
		result = 31 * result + score;
		result = 31 * result + ( delivery != null ? delivery.hashCode() : 0 );
		return result;
	}
}
