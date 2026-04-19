param(
    [switch]$NoClean
)

$ErrorActionPreference = "Continue"

Write-Host "Starting ParseBot admin in demo mode..."
try {
    & ./gradlew :admin:run --args="--demo"
} finally {
    if (-not $NoClean) {
        Write-Host "`nCleaning up demo logs..."
        foreach ($d in @("admin/logs", "service/logs", "logs")) {
            if (Test-Path $d) {
                Remove-Item -Recurse -Force $d -ErrorAction SilentlyContinue
                if (-not (Test-Path $d)) { Write-Host "  Removed $d" }
            }
        }
    }
}
