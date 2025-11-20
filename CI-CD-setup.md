Absolutely! Let’s create a **complete, professional CI/CD documentation** for your Android project using GitHub Actions, tailored to your workflow, including manual triggers, PR builds, tag-based releases, auto versioning, and artifact uploads.

---

# **CI/CD Documentation for Android Project using GitHub Actions**

## **Overview**

This CI/CD setup ensures:

* Fast, lightweight builds for **feature branches and pull requests**
* Manual triggers for saving CI resources
* Production-ready **signed APK/AAB releases** triggered by Git tags
* Automatic versioning based on Git tags and commit count
* Automatic upload of release artifacts to **GitHub Releases**

---

## **1. Workflow Structure**

The project uses **two main workflows**:

| Workflow   | Trigger                               | Purpose                                                     | Build Type      |
| ---------- | ------------------------------------- | ----------------------------------------------------------- | --------------- |
| Debug / CI | Push to any branch, PR to main        | Validate code, run fast debug builds                        | Debug APK       |
| Release    | Push Git tag (`v*`) or manual trigger | Produce signed release APK/AAB and upload to GitHub Release | Release APK/AAB |

---

## **2. Trigger Configuration**

### **Debug / PR Workflow**

```yaml
on:
  workflow_dispatch:   # Manual trigger
  push:               # Automatic trigger on all branches
    branches: ['**']
  pull_request:       # Automatic trigger for PRs
    branches: ['**']
```

* Runs **lightweight builds** for every commit or PR
* Ensures that `main` branch remains stable
* Manual trigger allows running CI on feature branches if needed

### **Release Workflow**

```yaml
on:
  workflow_dispatch:   # Manual release trigger
  push:
    tags:
      - 'v*'          # Trigger only for version tags
```

* Only runs for tagged commits (e.g., `v1.0.0`)
* Produces signed release artifacts
* Uploads APK/AAB to GitHub Release automatically

---

## **3. Keystore Management**

* **Keystore and passwords are stored as GitHub Secrets**:

| Secret Name         | Purpose                      |
| ------------------- | ---------------------------- |
| `KEYSTORE_BASE64`   | Base64-encoded keystore file |
| `KEYSTORE_PASSWORD` | Keystore password            |
| `KEY_ALIAS`         | Key alias                    |
| `KEY_PASSWORD`      | Key password                 |

* GitHub Actions decodes the keystore and generates `app/keystore.properties` at runtime.
* No sensitive files are stored in the repository.

---

## **4. Gradle Configuration for Auto Versioning**

`app/build.gradle.kts` dynamically sets versionName and versionCode:

```kotlin
val gitTag = findProperty("GIT_TAG") as? String ?: "0.0.0"
versionName = gitTag.removePrefix("v")

val versionCodeFromGit = "git rev-list --count HEAD".runCommand()
versionCode = versionCodeFromGit.toInt()

fun String.runCommand(): String =
    Runtime.getRuntime().exec(this).inputStream.bufferedReader().readText().trim()
```

* `versionName` comes from the Git tag (e.g., `v1.2.0` → `1.2.0`)
* `versionCode` is incremented automatically based on total commits

**GitHub Action passes the tag to Gradle:**

```yaml
- name: Build Signed Release
  run: ./gradlew clean assembleRelease bundleRelease --no-daemon -PGIT_TAG=${GITHUB_REF#refs/tags/}
  env:
    GITHUB_REF: ${{ github.ref }}
```

---

## **5. Debug / PR Workflow Example**

```yaml
name: CI

on:
  workflow_dispatch:
  push:
    branches: ['**']
  pull_request:
    branches: ['**']

jobs:
  debug-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: 17
          distribution: temurin
      - uses: android-actions/setup-android@v3
      - run: ./gradlew assembleDebug --no-daemon
```

**Key Points:**

* Fast Debug builds
* Runs for PRs and all branch pushes
* Optional manual trigger via `workflow_dispatch`

---

## **6. Release Workflow Example**

```yaml
name: Release Android App

on:
  workflow_dispatch:
  push:
    tags:
      - 'v*'

jobs:
  release-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: 17
          distribution: temurin
      - uses: android-actions/setup-android@v3

      - name: Prepare Keystore
        run: |
          mkdir -p app
          echo "$KEYSTORE_BASE64" | base64 --decode > app/release.jks
          cat <<EOF > app/keystore.properties
          storePassword=${{ secrets.KEYSTORE_PASSWORD }}
          keyPassword=${{ secrets.KEY_PASSWORD }}
          keyAlias=${{ secrets.KEY_ALIAS }}
          storeFile=release.jks
          EOF
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}

      - name: Build Signed Release
        run: ./gradlew clean assembleRelease bundleRelease --no-daemon -PGIT_TAG=${GITHUB_REF#refs/tags/}
        env:
          GITHUB_REF: ${{ github.ref }}

      - name: Upload APK/AAB to GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          files: |
            app/build/outputs/apk/release/*.apk
            app/build/outputs/bundle/release/*.aab
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

## **7. How to Use**

### **Feature development / PR**

1. Push code to a feature branch → Debug build runs automatically
2. Open a PR targeting `main` → Debug build validates the code
3. Optionally, trigger workflow manually for heavy testing

### **Release**

1. Tag the release:

```bash
git tag v1.2.0
git push origin v1.2.0
```

2. Workflow runs automatically
3. APK/AAB is generated with correct versionName/versionCode
4. Artifacts uploaded to **GitHub Release**

---

## **8. Checking Version Locally**

* Pass the Git tag manually:

```bash
./gradlew assembleRelease -PGIT_TAG=v1.2.0
```

* Use **APK Analyzer** in Android Studio to see `versionName` and `versionCode`
* Optional Gradle task:

```kotlin
tasks.register("printVersion") {
    doLast {
        println("versionName = ${android.defaultConfig.versionName}")
        println("versionCode = ${android.defaultConfig.versionCode}")
    }
}
```

```bash
./gradlew printVersion -PGIT_TAG=v1.2.0
```

---

## **9. Advantages of This CI/CD Setup**

* Lightweight CI for PRs and branches → saves CI time and resources
* Manual triggers for custom builds → full control
* Automatic, consistent versioning → avoids human errors
* Signed releases and GitHub artifact uploads → ready for testers or Play Store
* Professional, maintainable workflow → suitable for production

---