---
title: "Spring Boot 4.1 — MockMvc Moved to spring-boot-webmvc-test"
tags: [spring-boot, testing, mockmvc, java]
status: active
created: 2026-08-07
---

# Spring Boot 4.1 Test Changes (my-private-digital-school)

Repo uses Spring Boot 4.1.0 (see `build.gradle`, Java 25 toolchain).

## Gotcha: MockMvc is NOT on the test classpath by default

In Boot 4.x, `spring-boot-starter-test` no longer brings MockMvc auto-config.
`@AutoConfigureMockMvc` and the old package
`org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc` do NOT exist.

Verified: `spring-boot-test-autoconfigure-4.1.0.jar` contains no MockMvc/web classes.

### Fix (already applied)

1. `build.gradle`: added `testImplementation 'org.springframework.boot:spring-boot-webmvc-test'`
   (version resolved from the Boot BOM that the Boot Gradle plugin imports).
2. Import changed to the new package:
   `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`.

```java
@SpringBootTest(properties = { ... })  // + mock/disable beans as usual
@AutoConfigureMockMvc                   // new package
class MyWebTest { @Autowired MockMvc mockMvc; ... }
```

- `@SpringBootTest` in Boot 4 provides no MockMvc by itself — you MUST add
  `@AutoConfigureMockMvc` (per the official 4.0 migration guide).
- Reference: javadoc.io/doc/org.springframework.boot/spring-boot-webmvc-test (4.1.0);
  github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide.
