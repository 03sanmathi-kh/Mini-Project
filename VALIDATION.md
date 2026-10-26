# Validation record

Checked on 26 October 2026.

- React production build: passed (`npm run build`), 85 modules transformed.
- JSX parser check: passed.
- Flyway V1 -> V2 -> V3 execution on H2 PostgreSQL compatibility mode: passed.
- Spring Boot smoke startup: passed; 11 repositories discovered, all three migrations applied, Hibernate `ddl-auto=validate` passed, and Tomcat started.
- Backend tests: 16 passed, 0 failures, 0 errors, 0 skipped.
  - 10 MockMvc security integration tests.
  - 5 workflow/email integration tests, including the complete correction, second review round, approval, payment, final inspection, and closure path.
  - 1 focused ownership/assignment policy test.
- Packaged executable JAR created: `backend/target/permit-engine1-1.0.0.jar`.

The managed Windows sandbox runs JDK 21 from an `E:` shim and denies `ZipFileSystem.close()` canonical-path access to dependency jars. Consequently, a normal Maven lifecycle reports an `AccessDeniedException` after `javac` has emitted valid Java 17 classes. Validation used the emitted classes, ran Surefire directly (all tests passed), then ran the remaining Maven verify/package goals with compilation/test phases skipped. Docker and a normal JDK 17/IntelliJ environment do not use this sandbox path arrangement.

Not executed here: a live PostgreSQL server, Docker Desktop, or a manual multi-user browser presentation. PostgreSQL-specific DDL is limited to standard constructs supported by PostgreSQL; the same migration chain passed on the test database. The frontend production bundle and backend application startup were both executed successfully.
