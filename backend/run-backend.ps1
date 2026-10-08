# =============================================================================
# RescueRoute Backend PowerShell Runner
# Loads .env variables and launches Spring Boot with Maven Wrapper
# =============================================================================

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$EnvFile = Join-Path $ScriptDir ".env"

if (Test-Path $EnvFile) {
    Write-Host "[RescueRoute] Loading environment variables from .env..." -ForegroundColor Cyan
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            $parts = $line -split "=", 2
            if ($parts.Length -eq 2) {
                $varName = $parts[0].Trim()
                $varValue = $parts[1].Trim()
                [System.Environment]::SetEnvironmentVariable($varName, $varValue, "Process")
                Write-Host "  -> Set $varName" -ForegroundColor DarkGray
            }
        }
    }
} else {
    Write-Host "[RescueRoute] No .env file found. Using default environment variables." -ForegroundColor Yellow
}

Write-Host "[RescueRoute] Starting Spring Boot Backend on port 8080..." -ForegroundColor Green
& .\mvnw.cmd spring-boot:run
