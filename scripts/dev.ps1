
$ErrorActionPreference = "Stop"

# Resolve project root from this script's location.
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $ProjectRoot ".env"
$ComposeFile = Join-Path $ProjectRoot "compose.yml"

# Verify required files exist.
if (-not (Test-Path $EnvFile)) {
    throw "Missing .env file: $EnvFile"
}

if (-not (Test-Path $ComposeFile)) {
    throw "Missing docker-compose.yml: $ComposeFile"
}

# Load variables from .env into this PowerShell process.
Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()

    if ($line -and -not $line.StartsWith("#")) {
        $parts = $line -split "=", 2

        if ($parts.Count -eq 2) {
            $key = $parts[0].Trim()
            $value = $parts[1].Trim()

            if ($key -match '^[A-Za-z_][A-Za-z0-9_]*$') {
                [Environment]::SetEnvironmentVariable(
                    $key,
                    $value,
                    "Process"
                )
            }
        }
    }
}

# Ensure Java uses the supported PostgreSQL timezone.
$env:JAVA_TOOL_OPTIONS = "-Duser.timezone=Asia/Kolkata"

# Verify Docker is available.
docker info | Out-Null

if ($LASTEXITCODE -ne 0) {
    throw "Docker is not running. Start Docker Desktop and try again."
}

# Start PostgreSQL if it is not already running.
$container = docker ps `
    --filter "name=^/hr-postgres$" `
    --format "{{.Names}}"

if (-not $container) {
    Write-Host "Starting PostgreSQL..."

    docker compose -f $ComposeFile up -d postgres

    if ($LASTEXITCODE -ne 0) {
        throw "Failed to start PostgreSQL."
    }
}

# Wait until PostgreSQL accepts connections.
Write-Host "Waiting for PostgreSQL to become ready..."

$maxAttempts = 30
$attempt = 0

while ($attempt -lt $maxAttempts) {
    docker exec hr-postgres pg_isready `
        -U $env:POSTGRES_USER `
        -d $env:POSTGRES_DB | Out-Null

    if ($LASTEXITCODE -eq 0) {
        break
    }

    Start-Sleep -Seconds 2
    $attempt++
}

if ($attempt -ge $maxAttempts) {
    throw "PostgreSQL did not become ready within the expected time."
}

Write-Host "PostgreSQL is ready."

# Run the organization service.
Set-Location $ProjectRoot

Write-Host "Starting organization-service on port 8081..."

mvn -pl services/organization-service spring-boot:run
