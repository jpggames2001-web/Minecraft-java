param([string]$RepositoryUrl = 'https://github.com/jpggames2001-web/Minecraft-java.git')
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & mvn -B verify
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    if (-not (Test-Path '.git')) { & git init -b main; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }
    & git remote get-url origin 2>$null
    if ($LASTEXITCODE -ne 0) { & git remote add origin $RepositoryUrl; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }
    & git add .gitignore pom.xml README.md LICENSE docs scripts src .github downloads
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & git diff --cached --quiet
    if ($LASTEXITCODE -eq 1) { & git commit -m 'Implement procedural Java Skyblock game and validation'; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }
    elseif ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & git push -u origin HEAD:main
    exit $LASTEXITCODE
} finally { Pop-Location }
