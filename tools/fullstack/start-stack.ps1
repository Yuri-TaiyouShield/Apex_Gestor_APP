$ErrorActionPreference = 'Stop'

docker compose -f (Join-Path $PSScriptRoot 'docker-compose.yml') up -d

$healthUrl = 'http://localhost:8080/actuator/health'
for ($attempt = 1; $attempt -le 30; $attempt++) {
    try {
        $health = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 3
        if ($health.status -eq 'UP') {
            Write-Host "API pronta: $healthUrl"
            Write-Host 'Web Staff: http://localhost:4200'
            Write-Host 'Web Client: http://localhost:4201'
            exit 0
        }
    } catch {
        Start-Sleep -Seconds 2
    }
}

throw "A API nao ficou pronta em $healthUrl. Consulte: docker compose -f `"$PSScriptRoot\docker-compose.yml`" logs backend-api"
