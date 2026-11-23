# Registration defect and retest register

| ID | Observed failure | Correction | Actual retest |
|---|---|---|---|
| REG-D01 | Java Query annotation ambiguous between JPA and Spring Data | Explicit imports in registration repositories | Compiled in full verify and PostgreSQL run |
| REG-D02 | New integration test omitted MockHttpServletRequestBuilder import | Added correct test import | All registration test methods compiled and executed |
| REG-D03 | H2 preservation test queried unquoted lowercase Flyway metadata table | Quote Flyway table and column identifiers, preserving production SQL | Final full suite: 77 executed, 76 passed, 0 failed, 1 skipped; PostgreSQL prior lowercase query already passed |

These are observed implementation/test defects, not fabricated production incidents. Initial logs are retained, with sensitive test payload values redacted. No unresolved test failure remains in the final recorded suites. Guide acceptance and broader human/load validation remain separate.

REG-D04: The browser harness initially expected raw enum labels and synchronous activation completion. It was corrected to match visible labels and await dialog closure. The full Chrome flow then passed; no application defect is inferred from selector timing.
