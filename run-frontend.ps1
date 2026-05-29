Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectDir

Write-Host "Starting OES JavaFX client via Maven..."
Write-Host "Project: $projectDir"

& .\mvnw.cmd javafx:run
