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
package org.hibernate.search.test.bridge;

import java.util.Date;
import java.util.Calendar;
import java.net.URL;
import java.net.URI;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.*;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Cloud {
	
    private int id;
    private Long long1;
    private long long2;
    private Integer integerv1;
    private int integerv2;
    private Double double1;
    private double double2;
    private Float float1;
    private float float2;
    private String string;
    private Date myDate;
    private Date dateYear;
    private Date dateMonth;
    private Date dateDay;
    private Date dateHour;
    private Date dateMinute;
    private Date dateSecond;
    private Date dateMillisecond;
    private String customFieldBridge;
    private String customStringBridge;
    private Character char1;
    private char char2;
	private CloudType type;
	private boolean storm;
	private Class clazz;
	private URL url;
	private URI uri;
    private Calendar myCalendar;
    private Calendar calendarYear;
    private Calendar calendarMonth;
    private Calendar calendarDay;
    private Calendar calendarMinute;
    private Calendar calendarSecond;
    private Calendar calendarHour;
    private Calendar calendarMillisecond;

	@Field(index=Index.UN_TOKENIZED, store=Store.YES)
	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	@Field(index=Index.UN_TOKENIZED, store=Store.YES)
	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	@Field(index=Index.UN_TOKENIZED, store=Store.YES)
	public Class getClazz() {
		return clazz;
	}

	public void setClazz(Class clazz) {
		this.clazz = clazz;
	}

	@Field(index=Index.TOKENIZED, store=Store.YES)
    @FieldBridge(impl = TruncateFieldBridge.class)
    public String getCustomFieldBridge() {
        return customFieldBridge;
    }

    public void setCustomFieldBridge(String customFieldBridge) {
        this.customFieldBridge = customFieldBridge;
    }

    @Field(index=Index.TOKENIZED, store=Store.YES,
			bridge = @FieldBridge(impl = TruncateStringBridge.class, params = @Parameter( name="dividedBy", value="4" ) )
	)
    public String getCustomStringBridge() {
        return customStringBridge;
    }

    public void setCustomStringBridge(String customStringBridge) {
        this.customStringBridge = customStringBridge;
    }

    @Id @GeneratedValue @DocumentId
	public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    public Long getLong1() {
        return long1;
    }

    public void setLong1(Long long1) {
        this.long1 = long1;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    public long getLong2() {
        return long2;
    }

    public void setLong2(long long2) {
        this.long2 = long2;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    public Integer getIntegerv1() {
        return integerv1;
    }

    public void setIntegerv1(Integer integerv1) {
        this.integerv1 = integerv1;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    public int getIntegerv2() {
        return integerv2;
    }

    public void setIntegerv2(int integerv2) {
        this.integerv2 = integerv2;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    public Double getDouble1() {
        return double1;
    }

    public void setDouble1(Double double1) {
        this.double1 = double1;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    public double getDouble2() {
        return double2;
    }

    public void setDouble2(double double2) {
        this.double2 = double2;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    public Float getFloat1() {
        return float1;
    }

    public void setFloat1(Float float1) {
        this.float1 = float1;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    public float getFloat2() {
        return float2;
    }

    public void setFloat2(float float2) {
        this.float2 = float2;
    }

    @Field(index=Index.TOKENIZED, store=Store.YES)
	public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    public Date getMyDate() {
        return myDate;
    }

    public void setMyDate(Date myDate) {
        this.myDate = myDate;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    @DateBridge( resolution = Resolution.YEAR )
    public Date getDateYear() {
        return dateYear;
    }

    public void setDateYear(Date dateYear) {
        this.dateYear = dateYear;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    @DateBridge( resolution = Resolution.MONTH )
    public Date getDateMonth() {
        return dateMonth;
    }

    public void setDateMonth(Date dateMonth) {
        this.dateMonth = dateMonth;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    @DateBridge( resolution = Resolution.DAY )
    public Date getDateDay() {
        return dateDay;
    }

    public void setDateDay(Date dateDay) {
        this.dateDay = dateDay;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    @DateBridge( resolution = Resolution.HOUR )
    public Date getDateHour() {
        return dateHour;
    }

    public void setDateHour(Date dateHour) {
        this.dateHour = dateHour;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    @DateBridge( resolution = Resolution.MINUTE )
    public Date getDateMinute() {
        return dateMinute;
    }

    public void setDateMinute(Date dateMinute) {
        this.dateMinute = dateMinute;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    @DateBridge( resolution = Resolution.SECOND )
    public Date getDateSecond() {
        return dateSecond;
    }

    public void setDateSecond(Date dateSecond) {
        this.dateSecond = dateSecond;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
	@DateBridge( resolution = Resolution.MILLISECOND )
    public Date getDateMillisecond() {
        return dateMillisecond;
    }

    public void setDateMillisecond(Date dateMillisecond) {
        this.dateMillisecond = dateMillisecond;
    }

	@Field(index = Index.TOKENIZED, store = Store.YES)
	public CloudType getType() {
		return type;
	}

	public void setType(CloudType type) {
		this.type = type;
	}

	@Field(index = Index.TOKENIZED )
	public boolean isStorm() {
		return storm;
	}

	public void setStorm(boolean storm) {
		this.storm = storm;
	}
	
	@Field(index = Index.TOKENIZED, store = Store.YES)
	public Character getChar1() {
		return char1;
	}

	public void setChar1(Character char1) {
		this.char1 = char1;
	}

	@Field(index = Index.TOKENIZED, store = Store.YES)
	public char getChar2() {
		return char2;
	}

	public void setChar2(char char2) {
		this.char2 = char2;
	}

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    public Calendar getMyCalendar() {
        return myCalendar;
    }

    public void setMyCalendar(Calendar myCalendar) {
        this.myCalendar = myCalendar;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    @CalendarBridge(resolution = Resolution.YEAR )
    public Calendar getCalendarYear() {
        return calendarYear;
    }

    public void setCalendarYear(Calendar calendarYear) {
        this.calendarYear = calendarYear;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    @CalendarBridge( resolution = Resolution.MONTH )
    public Calendar getCalendarMonth() {
        return calendarMonth;
    }

    public void setCalendarMonth(Calendar calendarMonth) {
        this.calendarMonth = calendarMonth;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    @CalendarBridge( resolution = Resolution.DAY )
    public Calendar getCalendarDay() {
        return calendarDay;
    }

    public void setCalendarDay(Calendar calendarDay) {
        this.calendarDay = calendarDay;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    @CalendarBridge( resolution = Resolution.MINUTE )
    public Calendar getCalendarMinute() {
        return calendarMinute;
    }

    public void setCalendarMinute(Calendar calendarMinute) {
        this.calendarMinute = calendarMinute;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    @CalendarBridge( resolution = Resolution.HOUR )
    public Calendar getCalendarHour() {
        return calendarHour;
    }

    public void setCalendarHour(Calendar calendarHour) {
        this.calendarHour = calendarHour;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    @CalendarBridge( resolution = Resolution.MILLISECOND )
    public Calendar getCalendarMillisecond() {
        return calendarMillisecond;
    }

    public void setCalendarMillisecond(Calendar calendarMillisecond) {
        this.calendarMillisecond = calendarMillisecond;
    }

    @Field(index=Index.UN_TOKENIZED, store=Store.YES)
    @CalendarBridge( resolution = Resolution.SECOND )
    public Calendar getCalendarSecond() {
        return calendarSecond;
    }

    public void setCalendarSecond(Calendar calendarSecond) {
        this.calendarSecond = calendarSecond;
    }
}
