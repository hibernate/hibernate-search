package org.hibernate.search.test.bridge;

import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * @author Emmanuel Bernard
 */
public class StudentsSizeBridge implements TwoWayStringBridge {

    public Object stringToObject(String stringValue) {
        if (null == stringValue || stringValue.equals("")) {
            return 0;
        }
        return Integer.parseInt(stringValue);
    }

    public String objectToString(Object object) {
        if (object instanceof Teacher) {
            Teacher teacher = (Teacher) object;
            if (teacher.getStudents() != null && teacher.getStudents().size() > 0)
                return String.valueOf(teacher.getStudents().size());
            else
                return null;
        } else {
            throw new IllegalArgumentException(StudentsSizeBridge.class +
                    " used on a non-Teacher type: " + object.getClass());
        }
    }

}
