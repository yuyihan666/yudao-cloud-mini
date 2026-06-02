package cn.iocoder.yudao.framework.common.util.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class FastjsonClasspathTest {

    @Test
    void fastjsonShouldNotBeAvailableOnRuntimeClasspath() {
        assertFalse(isPresent("com.alibaba.fastjson.JSON"));
        assertFalse(isPresent("com.alibaba.fastjson2.JSON"));
        assertFalse(isPresent("com.alibaba.fastjson2.JSONReader"));
    }

    private static boolean isPresent(String className) {
        try {
            Class.forName(className, false, FastjsonClasspathTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

}
