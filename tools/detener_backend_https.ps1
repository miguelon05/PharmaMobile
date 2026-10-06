$ErrorActionPreference = 'Stop'
$pidFile = Join-Path $PSScriptRoot '../.local/backend-https.pid'
if (-not (Test-Path -LiteralPath $pidFile)) { throw 'No hay un lanzador HTTPS registrado.' }
$launcherPid = [int](Get-Content -LiteralPath $pidFile)
$launcher = Get-CimInstance Win32_Process -Filter "ProcessId = $launcherPid"
$expectedScript = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot 'iniciar_backend_https.ps1')).Path
if (-not $launcher) { Write-Output 'El lanzador ya no está activo.'; return }
if ($launcher.Name -ne 'powershell.exe' -or -not $launcher.CommandLine.Contains($expectedScript)) {
    throw 'El PID pertenece a otro proceso. No se detuvo nada.'
}
# Detiene exclusivamente el lanzador registrado y sus procesos Maven/Java hijos.
& taskkill.exe /PID $launcherPid /T /F
if ($LASTEXITCODE -ne 0) { throw 'No se pudo detener el lanzador HTTPS.' }
