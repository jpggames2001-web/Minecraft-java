param([Parameter(ValueFromRemainingArguments = $true)][string[]]$GameArgs)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    if (-not (Test-Path 'target/skyblock-classic-0.1.0.jar')) {
        & mvn -B verify
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
    & java -jar target/skyblock-classic-0.1.0.jar @GameArgs
    exit $LASTEXITCODE
} finally { Pop-Location }
