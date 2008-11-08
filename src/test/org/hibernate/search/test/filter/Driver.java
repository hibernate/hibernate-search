//$Id$
package org.hibernate.search.test.filter;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.FullTextFilterDefs;
import org.hibernate.search.annotations.FilterCacheModeType;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
@FullTextFilterDefs( {
		@FullTextFilterDef(name = "bestDriver", impl = BestDriversFilter.class, cache = FilterCacheModeType.NONE), //actual Filter implementation
		@FullTextFilterDef(name = "security", impl = SecurityFilterFactory.class, cache = FilterCacheModeType.INSTANCE_AND_DOCIDSETRESULTS), //Filter factory with parameters
		@FullTextFilterDef(name = "cacheresultstest", impl = ExcludeAllFilterFactory.class, cache = FilterCacheModeType.INSTANCE_AND_DOCIDSETRESULTS),
		@FullTextFilterDef(name = "cacheinstancetest", impl = InstanceBasedExcludeAllFilter.class, cache = FilterCacheModeType.INSTANCE_ONLY)
})
public class Driver {
	@Id
	@DocumentId
	private int id;
	@Field(index= Index.TOKENIZED)
	private String name;
	@Field(index= Index.UN_TOKENIZED)
	private String teacher;
	@Field(index= Index.UN_TOKENIZED)
	private int score;
	@Field(index= Index.UN_TOKENIZED)
	@DateBridge( resolution = Resolution.YEAR)
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

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		Driver driver = (Driver) o;

		if ( id != driver.id ) return false;
		if ( score != driver.score ) return false;
		if ( delivery != null ? !delivery.equals( driver.delivery ) : driver.delivery != null ) return false;
		if ( name != null ? !name.equals( driver.name ) : driver.name != null ) return false;
		return !( teacher != null ? !teacher.equals( driver.teacher ) : driver.teacher != null );

		}

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
