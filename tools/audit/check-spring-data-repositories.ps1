$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$javaRoot = Join-Path $repoRoot "Apex-Gestordemo\src\main\java"

if (-not (Test-Path $javaRoot)) {
    Write-Host "Backend Java nao encontrado em $javaRoot"
    exit 0
}

$patterns = "@Query", "nativeQuery", "createNativeQuery", "EntityManager", "Statement", "executeQuery"
$hits = Get-ChildItem -Path $javaRoot -Recurse -Filter *.java |
    Select-String -Pattern $patterns |
    Select-Object Path, LineNumber, Line

if ($hits) {
    Write-Host "Consultas manuais encontradas. Preferir convencoes Spring Data JPA:" -ForegroundColor Yellow
    $hits | Format-Table -Wrap
    exit 1
}

Write-Host "OK: repositorios sem consultas manuais detectadas." -ForegroundColor Green
