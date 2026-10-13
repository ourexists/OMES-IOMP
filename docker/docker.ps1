# OMES Docker — usage: .\docker\docker.ps1 <build|up|down> [infra]
param(
    [Parameter(Position = 0)]
    [ValidateSet("build", "up", "down")]
    [string]$Command,

    [Parameter(Position = 1)]
    [ValidateSet("infra", "all")]
    [string]$Target = "all",

    [switch]$SkipMaven,
    [switch]$SkipWeb
)

$ErrorActionPreference = "Stop"
if (-not $Command) {
    Write-Host @"
Usage: .\docker\docker.ps1 <build|up|down> [infra]
  build [-SkipMaven] [-SkipWeb]   package jars + vite dist + images
  up [infra|all]                  start (default all = infra + app)
  down                            stop & remove app stack
"@
    exit 1
}

$DeployRoot = $PSScriptRoot
$Root = Split-Path $DeployRoot -Parent
Push-Location $Root
try {
$WebRoot = Join-Path $Root "omes-iomp-web-admin"

if (-not (Test-Path ".env")) {
    Copy-Item (Join-Path $DeployRoot ".env.example") ".env"
    Write-Host "Created .env from .env.example"
}

function Invoke-Compose {
    & docker compose --env-file (Join-Path $Root ".env") -f (Join-Path $DeployRoot "docker-compose.yml") @args
    if ($LASTEXITCODE -ne 0) { throw "docker compose failed ($LASTEXITCODE)" }
}

switch ($Command) {
    "build" {
        foreach ($f in @("config\admin.properties", "config\sas.properties", "config\era-token.yml")) {
            if (-not (Test-Path $f)) { throw "Missing $f (copy from *.example if needed)" }
        }
        if (-not $SkipMaven) {
            Write-Host "==> mvn package"
            mvn -pl omes-iomp-runner-admin,omes-iomp-runner-sas -am clean package -DskipTests
            if ($LASTEXITCODE -ne 0) { throw "Maven failed" }
        }
        foreach ($module in @('omes-iomp-runner-admin', 'omes-iomp-runner-sas')) {
            $jars = @(Get-ChildItem "$module/target/$module-*.jar" -ErrorAction SilentlyContinue)
            if ($jars.Count -ne 1) { throw "Expected one executable JAR in $module/target; run a clean build" }
        }

        if (-not $SkipWeb) {
            if (-not (Test-Path $WebRoot)) { throw "Missing $WebRoot" }
            Write-Host "==> vite build → dist"
            Push-Location $WebRoot
            $previousSasBaseUrl = $env:VITE_SAS_BASE_URL
            try {
                if (-not (Test-Path node_modules)) {
                    npm ci --no-audit --no-fund
                    if ($LASTEXITCODE -ne 0) { throw "npm ci failed" }
                }
                $env:VITE_SAS_BASE_URL = ""
                npx vite build
                if ($LASTEXITCODE -ne 0) { throw "vite build failed" }
            } finally {
                $env:VITE_SAS_BASE_URL = $previousSasBaseUrl
                Pop-Location
            }
        }
        if (-not (Test-Path "$WebRoot\dist\index.html")) { throw "Missing frontend dist" }

        Write-Host "==> docker compose build"
        Invoke-Compose --profile app build
        Write-Host "OK → .\docker\docker.ps1 up"
    }
    "up" {
        if ($Target -eq "infra") {
            Invoke-Compose up -d postgres redis rabbitmq
        } else {
            Invoke-Compose --profile app up -d
        }
        Invoke-Compose ps
        Write-Host "Web :8080 | SAS :9400 | Admin :10010 | RabbitMQ mgmt :15672"
    }
    "down" {
        Invoke-Compose --profile app down
        Write-Host "Stopped."
    }
}
} finally {
    Pop-Location
}
