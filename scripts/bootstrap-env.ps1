#requires -Version 5.1
<#
.SYNOPSIS
Provisiona a toolchain local do Apex Gestor em Windows.

.DESCRIPTION
Script idempotente para mapear Docker, JDK, Node.js, NPM, Maven e CLIs globais
no PATH de maquina. Seguro para rodar multiplas vezes: entradas existentes nao
sao duplicadas e variaveis ja configuradas sao preservadas quando validas.

Use -CheckOnly para auditar sem alterar variaveis do sistema.
#>
[CmdletBinding()]
param(
    [switch]$CheckOnly,
    [switch]$SkipNpmGlobalInstall,
    [string]$NpmGlobalPrefix = "$env:ProgramData\npm-global"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Script:Changes = New-Object System.Collections.Generic.List[string]
$Script:Health = New-Object System.Collections.Generic.List[object]
$RepoRoot = Split-Path -Parent $PSScriptRoot

function Test-IsAdmin {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Write-Step([string]$Message) {
    Write-Host "[Apex Bootstrap] $Message" -ForegroundColor Cyan
}

function Convert-ToCanonicalPath([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return ""
    }

    $expanded = [Environment]::ExpandEnvironmentVariables($Value.Trim().Trim('"'))
    try {
        return ([IO.Path]::GetFullPath($expanded)).TrimEnd('\').ToLowerInvariant()
    } catch {
        return $expanded.TrimEnd('\').ToLowerInvariant()
    }
}

function Split-PathList([string]$RawPath) {
    if ([string]::IsNullOrWhiteSpace($RawPath)) {
        return @()
    }

    return $RawPath -split ';' |
        ForEach-Object { $_.Trim() } |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
}

function Test-PathEntryExists([string[]]$Entries, [string]$Candidate) {
    $candidateLiteral = $Candidate.Trim().TrimEnd('\')
    $candidateCanonical = Convert-ToCanonicalPath $Candidate

    foreach ($entry in $Entries) {
        if ($entry.Trim().TrimEnd('\').Equals($candidateLiteral, [StringComparison]::OrdinalIgnoreCase)) {
            return $true
        }

        if ((Convert-ToCanonicalPath $entry) -eq $candidateCanonical) {
            return $true
        }
    }

    return $false
}

function Set-MachineEnvironment([string]$Name, [string]$Value) {
    $current = [Environment]::GetEnvironmentVariable($Name, "Machine")
    if ($current -eq $Value) {
        $Script:Changes.Add("$Name ja esta configurado.")
    } elseif ($CheckOnly) {
        $Script:Changes.Add("CHECKONLY: configuraria $Name=$Value")
    } else {
        [Environment]::SetEnvironmentVariable($Name, $Value, "Machine")
        $Script:Changes.Add("Configurado $Name=$Value")
    }

    Set-Item -Path "Env:$Name" -Value $Value
}

function Add-MachinePathEntry([string]$Entry) {
    $expanded = [Environment]::ExpandEnvironmentVariables($Entry)
    if (-not (Test-Path -LiteralPath $expanded)) {
        $Script:Changes.Add("Ignorado PATH inexistente: $Entry")
        return
    }

    $machinePath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    $entries = @(Split-PathList $machinePath)
    if (Test-PathEntryExists $entries $Entry) {
        $Script:Changes.Add("PATH ja contem $Entry")
    } elseif ($CheckOnly) {
        $Script:Changes.Add("CHECKONLY: adicionaria $Entry ao PATH de maquina")
    } else {
        $newPath = (@($entries) + $Entry) -join ';'
        [Environment]::SetEnvironmentVariable("Path", $newPath, "Machine")
        $Script:Changes.Add("Adicionado ao PATH de maquina: $Entry")
    }

    $processEntry = [Environment]::ExpandEnvironmentVariables($Entry)
    $processEntries = @(Split-PathList $env:Path)
    if (-not (Test-PathEntryExists $processEntries $processEntry)) {
        $env:Path = (@($processEntries) + $processEntry) -join ';'
    }
}

function Resolve-CommandInfo([string[]]$Names) {
    foreach ($name in $Names) {
        $command = Get-Command $name -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($null -ne $command) {
            return $command
        }
    }

    return $null
}

function Get-JavaMajor([string]$JavaExe) {
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $versionText = (& $JavaExe -version 2>&1 | Out-String)
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    if ($versionText -match 'version "([^"]+)"') {
        $version = $Matches[1]
        if ($version.StartsWith("1.")) {
            return [int]($version.Split('.')[1])
        }

        return [int]($version.Split('.')[0])
    }

    return 0
}

function Find-JdkHome {
    $candidates = New-Object System.Collections.Generic.List[string]
    foreach ($value in @($env:JAVA_HOME, [Environment]::GetEnvironmentVariable("JAVA_HOME", "Machine"))) {
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            $candidates.Add($value)
        }
    }

    $javaCommand = Resolve-CommandInfo @("java.exe", "java")
    if ($null -ne $javaCommand) {
        $candidates.Add((Split-Path -Parent (Split-Path -Parent $javaCommand.Source)))
    }

    foreach ($root in @(
        "$env:ProgramFiles\Eclipse Adoptium",
        "$env:ProgramFiles\Java",
        "$env:ProgramFiles\Microsoft",
        "$env:ProgramFiles\BellSoft",
        "$env:ProgramFiles\Zulu"
    )) {
        if (Test-Path -LiteralPath $root) {
            Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match 'jdk|java|temurin|zulu|liberica' } |
                ForEach-Object { $candidates.Add($_.FullName) }
        }
    }

    $valid = foreach ($candidate in $candidates | Select-Object -Unique) {
        $javaExe = Join-Path $candidate "bin\java.exe"
        if (Test-Path -LiteralPath $javaExe) {
            $major = Get-JavaMajor $javaExe
            if ($major -ge 17) {
                [pscustomobject]@{ Home = $candidate; Major = $major }
            }
        }
    }

    return $valid | Sort-Object Major -Descending | Select-Object -First 1
}

function Find-NodeHome {
    $candidates = New-Object System.Collections.Generic.List[string]
    foreach ($value in @($env:NODE_HOME, [Environment]::GetEnvironmentVariable("NODE_HOME", "Machine"), "$env:ProgramFiles\nodejs", "${env:ProgramFiles(x86)}\nodejs")) {
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            $candidates.Add($value)
        }
    }

    $nodeCommand = Resolve-CommandInfo @("node.exe", "node")
    if ($null -ne $nodeCommand) {
        $candidates.Add((Split-Path -Parent $nodeCommand.Source))
    }

    foreach ($candidate in $candidates | Select-Object -Unique) {
        if ((Test-Path -LiteralPath (Join-Path $candidate "node.exe")) -and (Test-Path -LiteralPath (Join-Path $candidate "npm.cmd"))) {
            return $candidate
        }
    }

    return $null
}

function Find-DockerHome {
    foreach ($value in @($env:DOCKER_HOME, [Environment]::GetEnvironmentVariable("DOCKER_HOME", "Machine"), "$env:ProgramFiles\Docker\Docker")) {
        if (-not [string]::IsNullOrWhiteSpace($value) -and (Test-Path -LiteralPath (Join-Path $value "resources\bin\docker.exe"))) {
            return $value
        }
    }

    $dockerCommand = Resolve-CommandInfo @("docker.exe", "docker")
    if ($null -ne $dockerCommand) {
        $bin = Split-Path -Parent $dockerCommand.Source
        $resources = Split-Path -Parent $bin
        if ((Split-Path -Leaf $resources) -eq "resources") {
            return Split-Path -Parent $resources
        }
        return $bin
    }

    return $null
}

function Find-MavenHome {
    $candidates = New-Object System.Collections.Generic.List[string]
    foreach ($value in @($env:MAVEN_HOME, [Environment]::GetEnvironmentVariable("MAVEN_HOME", "Machine"))) {
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            $candidates.Add($value)
        }
    }

    $mvnCommand = Resolve-CommandInfo @("mvn.cmd", "mvn.bat", "mvn")
    if ($null -ne $mvnCommand) {
        $candidates.Add((Split-Path -Parent (Split-Path -Parent $mvnCommand.Source)))
    }

    foreach ($root in @("$env:ProgramFiles\Apache", "C:\tools", "C:\ProgramData\chocolatey\lib\maven")) {
        if (Test-Path -LiteralPath $root) {
            Get-ChildItem -LiteralPath $root -Directory -Recurse -Depth 2 -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match 'maven|apache-maven' } |
                ForEach-Object { $candidates.Add($_.FullName) }
        }
    }

    foreach ($candidate in $candidates | Select-Object -Unique) {
        if ((Test-Path -LiteralPath (Join-Path $candidate "bin\mvn.cmd")) -or (Test-Path -LiteralPath (Join-Path $candidate "bin\mvn.bat"))) {
            return $candidate
        }
    }

    return $null
}

function Ensure-NpmGlobalTool([string]$Package, [string[]]$Commands, [string]$DisplayName) {
    if ($SkipNpmGlobalInstall) {
        $Script:Changes.Add("Instalacao global ignorada por parametro: $DisplayName")
        return
    }

    if ($null -ne (Resolve-CommandInfo $Commands)) {
        $Script:Changes.Add("$DisplayName ja esta disponivel.")
        return
    }

    $npm = Resolve-CommandInfo @("npm.cmd", "npm")
    if ($null -eq $npm) {
        throw "npm nao encontrado no PATH. Corrija NODE_HOME antes de instalar CLIs globais."
    }

    if ($CheckOnly) {
        $Script:Changes.Add("CHECKONLY: executaria npm install -g $Package")
        return
    }

    Write-Step "Instalando $DisplayName via npm ($Package)"
    & $npm.Source install -g $Package
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao instalar $DisplayName via npm."
    }
}

function Add-HealthProbe([string]$Tool, [string[]]$Commands, [string[]]$Arguments) {
    $command = Resolve-CommandInfo $Commands
    if ($null -eq $command) {
        $Script:Health.Add([pscustomobject]@{
            Tool = $Tool
            Status = "MISSING"
            Version = "comando nao encontrado"
            Path = "-"
        })
        return
    }

    try {
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        try {
            $output = (& $command.Source @Arguments 2>&1 | Select-Object -First 3) -join ' | '
        } finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }

        $Script:Health.Add([pscustomobject]@{
            Tool = $Tool
            Status = "OK"
            Version = $output
            Path = $command.Source
        })
    } catch {
        $Script:Health.Add([pscustomobject]@{
            Tool = $Tool
            Status = "ERROR"
            Version = $_.Exception.Message
            Path = $command.Source
        })
    }
}

function Add-DirectHealthProbe([string]$Tool, [string]$ExecutablePath, [string[]]$Arguments) {
    if (-not (Test-Path -LiteralPath $ExecutablePath)) {
        $Script:Health.Add([pscustomobject]@{
            Tool = $Tool
            Status = "MISSING"
            Version = "arquivo nao encontrado"
            Path = $ExecutablePath
        })
        return
    }

    try {
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        try {
            $output = (& $ExecutablePath @Arguments 2>&1 | Select-Object -First 3) -join ' | '
        } finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }

        $Script:Health.Add([pscustomobject]@{
            Tool = $Tool
            Status = "OK"
            Version = $output
            Path = $ExecutablePath
        })
    } catch {
        $Script:Health.Add([pscustomobject]@{
            Tool = $Tool
            Status = "ERROR"
            Version = $_.Exception.Message
            Path = $ExecutablePath
        })
    }
}

if (-not $CheckOnly -and -not (Test-IsAdmin)) {
    throw "Execute este script em um PowerShell aberto como Administrador ou use -CheckOnly para auditoria sem alteracoes."
}

Write-Step "Iniciando provisionamento de ambiente Apex Gestor"
if ($CheckOnly) {
    Write-Step "Modo CheckOnly ativo: nenhuma variavel de sistema sera alterada."
}

$dockerHome = Find-DockerHome
if ($null -ne $dockerHome) {
    Set-MachineEnvironment "DOCKER_HOME" $dockerHome
    Add-MachinePathEntry "%DOCKER_HOME%\resources\bin"
} else {
    $Script:Changes.Add("Docker Desktop/Engine nao localizado. Instale Docker Desktop e rode novamente.")
}

$jdk = Find-JdkHome
if ($null -eq $jdk) {
    throw "JDK 17+ nao localizado. Instale Temurin/Microsoft/OpenJDK 17 ou superior e rode novamente."
}
Set-MachineEnvironment "JAVA_HOME" $jdk.Home
Add-MachinePathEntry "%JAVA_HOME%\bin"

$nodeHome = Find-NodeHome
if ($null -eq $nodeHome) {
    throw "Node.js com npm nao localizado. Instale Node.js 22 LTS ou superior e rode novamente."
}
Set-MachineEnvironment "NODE_HOME" $nodeHome
Add-MachinePathEntry "%NODE_HOME%"

if (-not (Test-Path -LiteralPath $NpmGlobalPrefix) -and -not $CheckOnly) {
    New-Item -ItemType Directory -Path $NpmGlobalPrefix -Force | Out-Null
}
Set-MachineEnvironment "NPM_CONFIG_PREFIX" $NpmGlobalPrefix
if ($CheckOnly -and -not (Test-Path -LiteralPath $NpmGlobalPrefix)) {
    $Script:Changes.Add("CHECKONLY: criaria pasta $NpmGlobalPrefix")
    $Script:Changes.Add("CHECKONLY: adicionaria %NPM_CONFIG_PREFIX% ao PATH de maquina")
    if (-not (Test-PathEntryExists @(Split-PathList $env:Path) $NpmGlobalPrefix)) {
        $env:Path = (@(Split-PathList $env:Path) + $NpmGlobalPrefix) -join ';'
    }
} else {
    Add-MachinePathEntry "%NPM_CONFIG_PREFIX%"
}

$mavenHome = Find-MavenHome
if ($null -ne $mavenHome) {
    Set-MachineEnvironment "MAVEN_HOME" $mavenHome
    Add-MachinePathEntry "%MAVEN_HOME%\bin"
} else {
    $mvnw = Join-Path $RepoRoot "Apex-Gestordemo\mvnw.cmd"
    if (Test-Path -LiteralPath $mvnw) {
        $Script:Changes.Add("Maven global nao encontrado; wrapper do projeto disponivel em $mvnw")
    } else {
        $Script:Changes.Add("Maven global e mvnw.cmd nao encontrados.")
    }
}

Ensure-NpmGlobalTool "@angular/cli@18" @("ng.cmd", "ng") "Angular CLI"
Ensure-NpmGlobalTool "@ionic/cli" @("ionic.cmd", "ionic") "Ionic CLI"
Ensure-NpmGlobalTool "electron" @("electron.cmd", "electron") "Electron CLI"
Ensure-NpmGlobalTool "electron-builder" @("electron-builder.cmd", "electron-builder") "Electron Builder"
Ensure-NpmGlobalTool "@electron-forge/cli" @("electron-forge.cmd", "electron-forge") "Electron Forge"

Write-Step "Resumo de alteracoes"
$Script:Changes | ForEach-Object { Write-Host " - $_" }

Write-Step "Health check final"
Add-HealthProbe "Docker" @("docker.exe", "docker") @("--version")
Add-HealthProbe "Docker Compose" @("docker.exe", "docker") @("compose", "version")
Add-HealthProbe "Java" @("java.exe", "java") @("-version")
Add-HealthProbe "Node.js" @("node.exe", "node") @("--version")
Add-HealthProbe "NPM" @("npm.cmd", "npm") @("--version")
Add-HealthProbe "Angular CLI" @("ng.cmd", "ng") @("version")
Add-HealthProbe "Ionic CLI" @("ionic.cmd", "ionic") @("--version")
Add-HealthProbe "Electron" @("electron.cmd", "electron") @("--version")
Add-HealthProbe "Electron Builder" @("electron-builder.cmd", "electron-builder") @("--version")
Add-HealthProbe "Electron Forge" @("electron-forge.cmd", "electron-forge") @("--version")
$mavenCommand = Resolve-CommandInfo @("mvn.cmd", "mvn.bat", "mvn")
if ($null -ne $mavenCommand) {
    Add-HealthProbe "Maven" @("mvn.cmd", "mvn.bat", "mvn") @("-version")
} else {
    Add-DirectHealthProbe "Maven Wrapper" (Join-Path $RepoRoot "Apex-Gestordemo\mvnw.cmd") @("-version")
}

$Script:Health | Format-Table -AutoSize

Write-Step "Concluido. Abra um novo terminal para carregar o PATH de maquina atualizado."
