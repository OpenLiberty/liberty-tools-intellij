package com.example.annotation;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;

import java.util.ArrayList;
import java.util.List;

/** Sample bean using Jakarta Annotations 2.1 lifecycle callbacks. */
@Priority(10)
public class LifecycleBean {

    private final List<String> events = new ArrayList<>();

    @PostConstruct
    void init() {
        events.add("init");
    }

    @PreDestroy
    void cleanup() {
        events.add("cleanup");
    }

    public List<String> getEvents() {
        return events;
    }
}
