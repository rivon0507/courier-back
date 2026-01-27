# Integration Test JWT Keys

**TEST-ONLY KEYS — NOT FOR PRODUCTION USE**

This directory contains RSA key pairs used **exclusively** for integration tests
(`integrationTest` source set).

## Why these keys exist

- Integration tests must be **deterministic** and **self-contained**
- Tests should run locally and in CI **without extra setup**
- Generating keys at runtime is intentionally avoided for integration tests

## Security guarantees

- These keys are **not included** in the application runtime classpath
- The `integrationTest` source set is **not packaged** into the production JAR
- Production profiles **must provide their own JWT keys explicitly**
- There is **no code path** allowing production to fall back to these files

## GitGuardian / Secret Scanners

Security scanners may flag these files as exposed private keys.
This is **expected and intentional**.

These keys:

- Protect no real data
- Are never used outside integration tests
- Can be considered permanently public test fixtures

Corresponding alerts should be **allowlisted / marked as acceptable risk**.

## Important

Never copy these keys into:

- `src/main/resources`
- environment variables
- deployment artifacts
- production or staging environments
