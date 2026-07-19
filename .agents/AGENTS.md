# Project-Specific Agent Rules

- **Version Increments**:
  - For minor changes, increase the version name by `+0.0.1` in the `app/build.gradle.kts` (or version-defining gradle file).
  - For major changes, increase the version name by `+0.1.0` and reset the last digit to `0` (e.g. from `1.0.1` to `1.1.0`).
- **Version Tracking**:
  - Keep the version format as `X.Y.Z` or `X.Y`.
- **Git Commits**:
  - Always stage all modified files (`git add .`).
  - Always commit the changes with a fitting descriptive message (`git commit -m "<message>"`).
- **Verification**:
  - Always attempt to compile and build the app (e.g., using `./gradlew assembleDebug` or equivalent) to verify correctness before finishing.

