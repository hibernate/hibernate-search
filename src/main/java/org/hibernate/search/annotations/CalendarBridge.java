package org.hibernate.search.annotations;

import java.lang.annotation.*;


/**
 * Defines the temporal resolution of a given field
 * Calendar are stored as String in GMT
 *
 * @author Amin Mohammed-Coleman 
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( {ElementType.FIELD, ElementType.METHOD} )
@Documented
public @interface CalendarBridge {
    Resolution resolution();
}
