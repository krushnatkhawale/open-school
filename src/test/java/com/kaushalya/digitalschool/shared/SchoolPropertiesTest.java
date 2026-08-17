package com.kaushalya.digitalschool.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;

class SchoolPropertiesTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(BindConfig.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SchoolProperties.class)
    static class BindConfig {
    }

    @Test
    void bindsConfiguredAcademicYearStartMonth() {
        context.withPropertyValues("school.academic-year-start-month=7")
                .run(ctx -> assertThat(ctx.getBean(SchoolProperties.class).academicYearStartMonth())
                        .isEqualTo(7));
    }

    @Test
    void defaultsAcademicYearStartMonthToAprilWhenAbsent() {
        context.run(ctx -> assertThat(ctx.getBean(SchoolProperties.class).academicYearStartMonth())
                .isEqualTo(4));
    }

    @Test
    void directBinderAppliesConfiguredValue() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("school.academic-year-start-month", "9");

        SchoolProperties properties = Binder.get(env)
                .bind("school", Bindable.of(SchoolProperties.class))
                .get();

        assertThat(properties.academicYearStartMonth()).isEqualTo(9);
    }

    @Test
    void recordExposesNoSetter() throws NoSuchMethodException {
        assertThat(SchoolProperties.class.getMethod("academicYearStartMonth"))
                .isNotNull();
        assertThat(java.util.Arrays.stream(SchoolProperties.class.getMethods())
                .noneMatch(m -> m.getName().equals("setAcademicYearStartMonth")))
                .isTrue();
    }
}
