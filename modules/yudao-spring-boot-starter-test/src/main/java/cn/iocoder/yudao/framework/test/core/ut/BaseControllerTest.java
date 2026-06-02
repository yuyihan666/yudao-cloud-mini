package cn.iocoder.yudao.framework.test.core.ut;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Controller slice test base.
 *
 * <p>Provides a shared {@link MockMvc} and a pre-configured {@link ObjectMapper}.
 * Note: the ObjectMapper is manually constructed (JavaTimeModule, NON_NULL inclusion,
 * no timestamp dates) and does <b>not</b> reflect the application's Spring-configured
 * Jackson settings. If you need the production ObjectMapper, inject it via
 * {@code @Autowired ObjectMapper} in your test class instead.
 */
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

    protected String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

}
