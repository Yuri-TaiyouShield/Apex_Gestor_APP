param(
    [switch]$SkipBuild,
    [switch]$Strict
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$excluded = @("\\.git\\", "\\node_modules\\", "\\target\\", "\\dist\\", "\\release\\", "\\runtime-logs\\", "\\run-logs\\")

function Write-Section($title) {
    Write-Host ""
    Write-Host "== $title ==" -ForegroundColor Cyan
}

function Get-TrackedTextFiles {
    git -C $repoRoot ls-files |
        Where-Object {
            $path = $_
            -not ($excluded | Where-Object { $path -match $_ }) -and
            $path -match "\.(java|ts|js|json|yml|yaml|properties|xml|md|sql|ps1|html|css)$"
        } |
        ForEach-Object { Join-Path $repoRoot $_ }
}

$failures = New-Object System.Collections.Generic.List[string]

Write-Section "Secret scan"
$secretPatterns = @(
    "AKIA[0-9A-Z]{16}",
    "-----BEGIN (RSA|OPENSSH|EC|DSA)? ?PRIVATE KEY-----",
    "(?i)(password|passwd|pwd|secret|token|api[_-]?key)\s*[:=]\s*['""][^'""]{8,}"
)
$secretHits = Get-TrackedTextFiles | Select-String -Pattern $secretPatterns -AllMatches
if ($secretHits) {
    $secretHits | Select-Object Path, LineNumber, Line | Format-Table -Wrap
    $failures.Add("Possiveis segredos hardcoded encontrados.")
} else {
    Write-Host "Nenhum segredo obvio encontrado em arquivos versionados."
}

Write-Section "Spring Data JPA guardrail"
$queryHits = Get-ChildItem -Path (Join-Path $repoRoot "Apex-Gestordemo\src\main\java") -Recurse -Filter *.java -ErrorAction SilentlyContinue |
    Select-String -Pattern "@Query", "nativeQuery", "createNativeQuery", "Statement", "executeQuery"
if ($queryHits) {
    $queryHits | Select-Object Path, LineNumber, Line | Format-Table -Wrap
    $failures.Add("Ha consultas manuais ou APIs SQL diretas para revisar.")
} else {
    Write-Host "Repositorios limpos: sem @Query/nativeQuery/Statement detectados."
}

Write-Section "Security posture"
$securityFile = Join-Path $repoRoot "Apex-Gestordemo\src\main\java\com\Apex\Apex_Gestordemo\SecurityConfig.java"
if (Test-Path $securityFile) {
    $securityText = Get-Content $securityFile -Raw
    if ($securityText -match "anyRequest\(\)\.permitAll\(\)" -and $securityText -notmatch "requireAuthentication") {
        $failures.Add("SecurityConfig expoe toda a API sem chave de producao.")
    }
    if ($securityText -notmatch "contentSecurityPolicy") {
        $failures.Add("Headers de seguranca HTTP ausentes.")
    }
    Write-Host "SecurityConfig auditado."
}

if (-not $SkipBuild) {
    Write-Section "Backend build/test"
    Push-Location (Join-Path $repoRoot "Apex-Gestordemo")
    try {
        if (Test-Path ".\mvnw.cmd") {
            cmd /c mvnw.cmd -q test
            cmd /c mvnw.cmd -q -DskipTests package
        } else {
            mvn -q test
            mvn -q -DskipTests package
        }
    } finally {
        Pop-Location
    }
}

Write-Section "Result"
if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Host "- $_" -ForegroundColor Yellow }
    if ($Strict) {
        exit 1
    }
    Write-Host "Auditoria finalizada com alertas. Use -Strict para falhar o pipeline." -ForegroundColor Yellow
} else {
    Write-Host "Auditoria enterprise sem falhas bloqueantes." -ForegroundColor Green
}
