[CmdletBinding()]
param(
    [string]$OutputDirectory = ".\backups",
    [int]$RetentionDays = 14
)

$ErrorActionPreference = "Stop"

foreach ($name in @("DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASSWORD")) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Variavel obrigatoria ausente: $name"
    }
}

$dumpCommand = Get-Command mysqldump -ErrorAction Stop
$resolvedOutput = [IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $resolvedOutput | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$dumpFile = Join-Path $resolvedOutput "apex-$timestamp.sql"
$previousMysqlPassword = [Environment]::GetEnvironmentVariable("MYSQL_PWD", "Process")

try {
    [Environment]::SetEnvironmentVariable("MYSQL_PWD", $env:DB_PASSWORD, "Process")
    & $dumpCommand.Source `
        --host=$env:DB_HOST `
        --port=$env:DB_PORT `
        --user=$env:DB_USER `
        --database=$env:DB_NAME `
        --single-transaction `
        --routines `
        --events `
        --triggers `
        --hex-blob `
        --set-gtid-purged=OFF `
        --result-file=$dumpFile
    if ($LASTEXITCODE -ne 0) {
        throw "mysqldump terminou com codigo $LASTEXITCODE"
    }
}
finally {
    [Environment]::SetEnvironmentVariable("MYSQL_PWD", $previousMysqlPassword, "Process")
}

$file = Get-Item -LiteralPath $dumpFile
if ($file.Length -le 0) {
    throw "Backup vazio: $dumpFile"
}

$checksum = (Get-FileHash -LiteralPath $dumpFile -Algorithm SHA256).Hash
"$checksum *$($file.Name)" | Set-Content -LiteralPath "$dumpFile.sha256" -Encoding ASCII

$cutoff = (Get-Date).AddDays(-1 * [Math]::Max(1, $RetentionDays))
Get-ChildItem -LiteralPath $resolvedOutput -Filter "apex-*.sql" -File |
    Where-Object { $_.LastWriteTime -lt $cutoff } |
    ForEach-Object { Remove-Item -LiteralPath $_.FullName }

Write-Output "Backup criado: $dumpFile ($($file.Length) bytes)"
Write-Output "SHA-256: $checksum"
Write-Output "Retencao aplicada: $RetentionDays dias"
