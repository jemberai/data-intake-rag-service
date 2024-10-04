package org.jemberai.dataintake.utils;
/*
 * Created by Ashok Kumar Pant
 * Email: asokpant@gmail.com
 * Created on 04/10/2024.
 */

public class StringUtil {
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}
