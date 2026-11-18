[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$SqlFile,
    [switch]$AllowDataLoss
)

$ErrorActionPreference = "Stop"

if (-not $AllowDataLoss) {
    throw "Restore sobrescreve dados. Reexecute com -AllowDataLoss apos confirmar o banco alvo."
}

$resolvedSqlFile = [IO.Path]::GetFullPath($SqlFile)
if (-not (Test-Path -LiteralPath $resolvedSqlFile -PathType Leaf)) {
    throw "Arquivo de backup nao encontrado: $resolvedSqlFile"
}

foreach ($name in @("DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASSWORD")) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Variavel obrigatoria ausente: $name"
    }
}

$mysqlCommand = Get-Command mysql -ErrorAction Stop
$previousMysqlPassword = [Environment]::GetEnvironmentVariable("MYSQL_PWD", "Process")

try {
    [Environment]::SetEnvironmentVariable("MYSQL_PWD", $env:DB_PASSWORD, "Process")
    $mysqlArguments = @(
        "--host=$env:DB_HOST",
        "--port=$env:DB_PORT",
        "--user=$env:DB_USER",
        "--database=$env:DB_NAME",
        "--binary-mode=1"
    )
    Get-Content -LiteralPath $resolvedSqlFile -ReadCount 1000 |
        & $mysqlCommand.Source @mysqlArguments
    if ($LASTEXITCODE -ne 0) {
        throw "mysql terminou com codigo $LASTEXITCODE"
    }
}
finally {
    [Environment]::SetEnvironmentVariable("MYSQL_PWD", $previousMysqlPassword, "Process")
}

Write-Output "Restore aplicado em $($env:DB_HOST):$($env:DB_PORT)/$($env:DB_NAME)"
Write-Output "Execute a validacao descrita em docs/operacao/backup-restore.md antes de liberar trafego."
