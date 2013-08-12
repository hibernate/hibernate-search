/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.filter;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FilterCacheModeType;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.FullTextFilterDefs;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
@FullTextFilterDefs({
		@FullTextFilterDef(name = "bestDriver", impl = BestDriversFilter.class, cache = FilterCacheModeType.NONE),
		//actual Filter implementation
		@FullTextFilterDef(name = "security",
				impl = SecurityFilterFactory.class,
				cache = FilterCacheModeType.INSTANCE_AND_DOCIDSETRESULTS),
		@FullTextFilterDef(name = "fieldConstraintFilter-1",
				impl = FieldConstraintFilter.class,
				cache = FilterCacheModeType.INSTANCE_AND_DOCIDSETRESULTS),
		@FullTextFilterDef(name = "fieldConstraintFilter-2",
				impl = FieldConstraintFilter.class,
				cache = FilterCacheModeType.INSTANCE_AND_DOCIDSETRESULTS),
		//Filter factory with parameters
		@FullTextFilterDef(name = "cacheresultstest",
				impl = ExcludeAllFilterFactory.class,
				cache = FilterCacheModeType.INSTANCE_AND_DOCIDSETRESULTS),
		@FullTextFilterDef(name = "cacheinstancetest",
				impl = InstanceBasedExcludeAllFilter.class,
				cache = FilterCacheModeType.INSTANCE_ONLY),
		@FullTextFilterDef(name = "empty",
				impl = NullReturningEmptyFilter.class,
				cache = FilterCacheModeType.INSTANCE_ONLY)
})
public class Driver {
	@Id
	@DocumentId
	private int id;
	@Field(analyze = Analyze.YES)
	private String name;
	@Field(analyze = Analyze.NO)
	private String teacher;
	@Field(analyze = Analyze.NO)
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
