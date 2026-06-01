package cn.iocoder.yudao.framework.test.core.builder;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestDataBuilderTest {

    static class PageParamBuilder extends TestDataBuilder<PageParam, PageParamBuilder> {

        PageParamBuilder() {
            super(new PageParam().setPageNo(1).setPageSize(10));
        }

        PageParamBuilder pageNo(Integer pageNo) {
            data.setPageNo(pageNo);
            return self();
        }

        PageParamBuilder pageSize(Integer pageSize) {
            data.setPageSize(pageSize);
            return self();
        }

    }

    @Test
    void should_build_with_readable_defaults() {
        PageParam result = new PageParamBuilder().build();

        assertEquals(1, result.getPageNo());
        assertEquals(10, result.getPageSize());
    }

    @Test
    void should_override_defaults() {
        PageParam result = new PageParamBuilder()
                .pageNo(2)
                .pageSize(50)
                .build();

        assertEquals(2, result.getPageNo());
        assertEquals(50, result.getPageSize());
    }

}
