param(
    [string]$Backend = (Join-Path $PSScriptRoot '../../pharmaSoft'),
    [string]$JavaHome = 'C:\Program Files\Java\jdk-21'
)
$ErrorActionPreference = 'Stop'
$backendPath = (Resolve-Path -LiteralPath $Backend).Path
if (-not (Test-Path -LiteralPath (Join-Path $backendPath '.local/https/server-key.pem'))) {
    throw 'Primero ejecuta tools/preparar_https.py con la ruta del backend.'
}
$env:JAVA_HOME = $JavaHome
Push-Location -LiteralPath $backendPath
try {
    & .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev,https' '-Dspring-boot.run.jvmArguments=-Xmx512m'
    if ($LASTEXITCODE -ne 0) { throw "El backend termino con codigo $LASTEXITCODE" }
} finally {
    Pop-Location
}
