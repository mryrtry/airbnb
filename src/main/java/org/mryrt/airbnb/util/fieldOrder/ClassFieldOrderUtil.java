package org.mryrt.airbnb.util.fieldOrder;

import org.springframework.core.MethodParameter;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ClassFieldOrderUtil {

    public static List<String> getFieldOrderFromClass(MethodParameter parameter) {
        if (parameter == null) {
            return Collections.emptyList();
        } else {
            parameter.getParameterType();
        }

        Class<?> targetClass = parameter.getParameterType();
        return Arrays.stream(targetClass.getDeclaredFields()).map(Field::getName).collect(Collectors.toList());
    }

}
