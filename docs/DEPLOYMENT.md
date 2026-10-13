# GitHub deployment

The cloud checkout already has an `origin` for `jpggames2001-web/Minecraft-java` and an unborn local branch named `work`. You do not need to recreate the repository or reset the branch. Publishing is an explicit action using your Git authentication. The publication commands below commit and push changes only when explicitly invoked.

## Bash: Arch Linux or this cloud checkout

```bash
cd /workspace/Minecraft-java             # or your local checkout path
bash scripts/maven.sh -B verify
git status --short
# Only when starting outside an existing Git checkout:
# git init -b main
# git remote add origin https://github.com/jpggames2001-web/Minecraft-java.git

git add .gitignore pom.xml README.md LICENSE docs scripts src .github downloads
git commit -m "Implement procedural Java Skyblock game and validation"
git push -u origin HEAD:main
```

The bundled initialization/commit/push script performs that flow and initializes a repository only when one does not already exist:

```bash
bash scripts/publish.sh
# Supply a URL only for a checkout that has no origin:
bash scripts/publish.sh https://github.com/YOUR_ACCOUNT/YOUR_REPOSITORY.git
```

## Windows PowerShell

```powershell
Set-Location C:\path\to\Minecraft-java
mvn -B verify
if ($LASTEXITCODE -ne 0) { throw "Build failed" }
git status --short
git add .gitignore pom.xml README.md LICENSE docs scripts src .github downloads
git commit -m "Implement procedural Java Skyblock game and validation"
git push -u origin HEAD:main
```

Or:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/publish.ps1
# For a new checkout with no origin:
powershell -ExecutionPolicy Bypass -File scripts/publish.ps1 -RepositoryUrl https://github.com/YOUR_ACCOUNT/YOUR_REPOSITORY.git
```

These scripts keep an existing origin, do not force-push and stop when build/commit/push fails. If `main` already contains remote work, integrate it normally before pushing; do not force-push to bypass the rejection. Git identity (`user.name` and `user.email`) must be configured by the person publishing; the scripts do not invent an identity or store tokens. On the cloud platform, use the existing Git proxy authentication rather than extracting credentials.

## Build artifacts

Run `mvn -B clean verify` separately on each platform. The Windows profile selects Windows natives; the Linux profile selects Linux natives. For a cross-built Windows JAR from Linux:

```bash
bash scripts/maven.sh -B -Dlwjgl.natives=natives-windows clean verify
```

This packages Windows binaries; it does not prove execution on Windows. Rebuild without the property for Linux afterward.

GitHub Actions is configured for Linux and Windows builds and a Linux Xvfb graphics test. It uploads the platform-specific JAR and test reports; the Linux job also uploads screenshots. The workflow will run only after the files are pushed to GitHub. CI results are separate from local validation.

To run an uploaded platform-matched artifact:

```bash
java -jar skyblock-classic-0.1.0.jar
```

No account/login service, backend, secret environment variable or original asset bundle is needed. Java 21 and a supported desktop graphics driver remain runtime requirements.
