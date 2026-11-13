$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# Neste iniciador local, usa POSTGRES_PASSWORD do .env em vez da
# DB_PASSWORD herdada do Windows. Restaura o ambiente ao terminar.
$senhaHerdada = $env:DB_PASSWORD
try {
    Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
    if (-not (Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue)) {
        Start-Process -FilePath 'cmd.exe' -ArgumentList '/c mvnw.cmd spring-boot:run > backend-local.log 2>&1' -WorkingDirectory $PSScriptRoot -WindowStyle Hidden
    }
    if (-not (Get-NetTCPConnection -State Listen -LocalPort 5173 -ErrorAction SilentlyContinue)) {
        Start-Process -FilePath 'cmd.exe' -ArgumentList '/c npm run dev > vite-local.log 2>&1' -WorkingDirectory (Join-Path $PSScriptRoot 'frontend') -WindowStyle Hidden
    }
} finally {
    $env:DB_PASSWORD = $senhaHerdada
}
Write-Host 'Projeto: http://localhost:5173. Aguarde o backend iniciar; consulte backend-local.log em caso de falha.'
