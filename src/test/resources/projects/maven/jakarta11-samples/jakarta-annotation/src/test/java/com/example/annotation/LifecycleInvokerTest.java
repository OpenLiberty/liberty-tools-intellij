package com.example.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.Priority;
import java.util.List;
import org.junit.jupiter.api.Test;

class LifecycleInvokerTest {

    @Test
    void invokesPostConstructAndPreDestroy() throws Exception {
        LifecycleBean bean = LifecycleInvoker.create(LifecycleBean.class);
        assertEquals(List.of("init"), bean.getEvents());

        LifecycleInvoker.destroy(bean);
        assertEquals(List.of("init", "cleanup"), bean.getEvents());
    }

    @Test
    void readsPriority() {
        assertEquals(10, LifecycleBean.class.getAnnotation(Priority.class).value());
    }
}
