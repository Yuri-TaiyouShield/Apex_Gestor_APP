param(
    [switch]$SkipBuild,
    [switch]$Strict
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path (Join-Path $PSScriptRoot "..") "..")
$excluded = @("/.git/", "/node_modules/", "/target/", "/dist/", "/release/", "/runtime-logs/", "/run-logs/")

function Write-Section($title) {
    Write-Host ""
    Write-Host "== $title ==" -ForegroundColor Cyan
}

function Get-RepositoryTextFiles {
    git -C $repoRoot -c core.quotePath=false ls-files --cached --others --exclude-standard |
        Where-Object {
            $path = $_
            $normalizedPath = "/" + ($path -replace "\\", "/")
            -not ($excluded | Where-Object { $normalizedPath.Contains($_) }) -and
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
$secretHits = Get-RepositoryTextFiles | Select-String -Pattern $secretPatterns -AllMatches
if ($secretHits) {
    $secretHits | Select-Object Path, LineNumber, Line | Format-Table -Wrap
    $failures.Add("Possiveis segredos hardcoded encontrados.")
} else {
    Write-Host "Nenhum segredo obvio encontrado no working tree elegivel."
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

Write-Section "Flyway migration versions"
$migrationRoot = Join-Path $repoRoot "Apex-Gestordemo\src\main\resources\db\migration"
$migrationFiles = Get-ChildItem -Path $migrationRoot -File -Filter "*.sql" -ErrorAction SilentlyContinue
$migrationGroups = $migrationFiles |
    Where-Object { $_.Name -match '^V([^_]+)__' } |
    Group-Object { $matches[1] }
$duplicateMigrations = $migrationGroups | Where-Object { $_.Count -gt 1 }
if ($duplicateMigrations) {
    foreach ($group in $duplicateMigrations) {
        $names = $group.Group.Name -join ", "
        $failures.Add("Versao Flyway duplicada na migration principal: V$($group.Name) ($names).")
        Write-Host "Versao duplicada: V$($group.Name) -> $names" -ForegroundColor Yellow
    }
} else {
    Write-Host "Nenhuma versao Flyway duplicada na migration principal."
}

Write-Section "Flyway location selection"
$applicationProperties = Join-Path $repoRoot "Apex-Gestordemo\src\main\resources\application.properties"
$mysqlMigrationRoot = Join-Path $repoRoot "Apex-Gestordemo\src\main\resources\db\migration-mysql"
if ((Test-Path $applicationProperties) -and (Test-Path $mysqlMigrationRoot)) {
    $applicationText = Get-Content -LiteralPath $applicationProperties -Raw
    if ($applicationText -match 'spring\.flyway\.locations=\$\{FLYWAY_LOCATIONS:classpath:db/migration\}') {
        $failures.Add("O perfil principal fixa classpath:db/migration apesar de existir uma familia db/migration-mysql selecionavel.")
        Write-Host "Localizacao Flyway explicita pode impedir a selecao da familia MySQL." -ForegroundColor Yellow
    } else {
        Write-Host "A localizacao Flyway nao fixa a familia principal quando ha variante MySQL."
    }
}

Write-Section "Flyway family parity"
if (Test-Path $mysqlMigrationRoot) {
    $mysqlMigrationFiles = Get-ChildItem -Path $mysqlMigrationRoot -File -Filter "*.sql" -ErrorAction SilentlyContinue
    $mainVersions = @($migrationFiles | ForEach-Object {
        if ($_.Name -match '^V([^_]+)__') { $matches[1] }
    } | Sort-Object -Unique)
    $mysqlVersions = @($mysqlMigrationFiles | ForEach-Object {
        if ($_.Name -match '^V([^_]+)__') { $matches[1] }
    } | Sort-Object -Unique)
    $missingInMysql = @($mainVersions | Where-Object { $_ -notin $mysqlVersions })
    $missingInMain = @($mysqlVersions | Where-Object { $_ -notin $mainVersions })
    if ($missingInMysql.Count -gt 0 -or $missingInMain.Count -gt 0) {
        if ($missingInMysql.Count -gt 0) {
            $failures.Add("Versoes Flyway ausentes na familia MySQL: V$($missingInMysql -join ', V').")
        }
        if ($missingInMain.Count -gt 0) {
            $failures.Add("Versoes Flyway ausentes na familia principal: V$($missingInMain -join ', V').")
        }
        Write-Host "As familias Flyway possuem conjuntos de versoes diferentes." -ForegroundColor Yellow
    } else {
        Write-Host "As familias Flyway possuem o mesmo conjunto de versoes: V$($mainVersions -join ', V')."
    }
}

Write-Section "JPA table coverage"
$migrationSql = (Get-ChildItem -Path $migrationRoot -File -Filter "*.sql" -ErrorAction SilentlyContinue |
    ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw }) -join "`n"
# Nao considere comentarios como evidencia de que uma tabela existe. A
# cobertura deve comparar apenas nomes presentes em statements DDL/DML de tabela.
$migrationSql = [regex]::Replace($migrationSql, '(?ms)--[^\r\n]*|/\*.*?\*/', '')
$migrationTables = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
$tableStatementPattern = '(?im)\b(?:CREATE\s+TABLE|ALTER\s+TABLE|INSERT\s+INTO)\s+(?:IF\s+NOT\s+EXISTS\s+)?([A-Za-z0-9_]+)'
foreach ($tableMatch in [regex]::Matches($migrationSql, $tableStatementPattern)) {
    [void]$migrationTables.Add($tableMatch.Groups[1].Value)
}
$entityRoot = Join-Path $repoRoot "Apex-Gestordemo\src\main\java\Model"
$entityFiles = Get-ChildItem -Path $entityRoot -File -Filter "*.java" -ErrorAction SilentlyContinue
$missingEntityTables = @()
foreach ($entityFile in $entityFiles) {
    $entityText = Get-Content -LiteralPath $entityFile.FullName -Raw
    if ($entityText -match '@Table\(name\s*=\s*"([^"]+)"') {
        $tableName = $matches[1]
        if (-not $migrationTables.Contains($tableName)) {
            $missingEntityTables += "$($entityFile.BaseName) -> $tableName"
        }
    }
}
if ($missingEntityTables.Count -gt 0) {
    $missingEntityTables | ForEach-Object { Write-Host "Tabela JPA sem referencia nas migrations: $_" -ForegroundColor Yellow }
    $failures.Add("Uma ou mais tabelas declaradas por entidades JPA nao aparecem na migration principal.")
} else {
    Write-Host "Todas as tabelas JPA declaradas possuem referencia na migration principal."
}

Write-Section "Hibernate schema posture"
$mainResourceRoot = Join-Path $repoRoot "Apex-Gestordemo\src\main\resources"
$unsafeDdlHits = Get-ChildItem -Path $mainResourceRoot -Recurse -File -Include *.properties,*.yml,*.yaml -ErrorAction SilentlyContinue |
    Select-String -Pattern "(?i)spring\.jpa\.hibernate\.ddl-auto\s*[:=]\s*(update|create|create-drop)"
if ($unsafeDdlHits) {
    $unsafeDdlHits | Select-Object Path, LineNumber, Line | Format-Table -Wrap
    $failures.Add("Configuracao de producao/local permite Hibernate criar ou atualizar schema automaticamente.")
} else {
    Write-Host "Nenhum perfil principal permite ddl-auto update/create/create-drop."
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
