param(
    [Parameter(Mandatory=$true)][string]$Version,
    [string]$NotesFile,
    [string]$Notes,
    [switch]$Prerelease,
    [switch]$SkipPush
)

$ErrorActionPreference = "Stop"

$tag = if ($Version.StartsWith("v")) { $Version } else { "v$Version" }
$bareVersion = $tag.TrimStart("v")

# --- Verify build.gradle version matches ---
$buildGradle = Get-Content "build.gradle" -Raw
$versionMatch = [regex]::Match($buildGradle, "version\s*=\s*'([^']+)'")
if (-not $versionMatch.Success) {
    Write-Error "Could not find version in build.gradle"
    exit 1
}
$gradleVersion = $versionMatch.Groups[1].Value
if ($gradleVersion -ne $bareVersion) {
    Write-Error "build.gradle version is '$gradleVersion' but release version is '$bareVersion'. Update build.gradle first."
    exit 1
}

# --- Verify gh CLI is authenticated ---
Write-Host "Checking gh CLI auth..."
$ghStatus = & gh auth status 2>&1 | Out-String
if ($LASTEXITCODE -ne 0) {
    Write-Error "gh CLI is not authenticated. Run 'gh auth login' first."
    Write-Host $ghStatus
    exit 1
}

# --- Check working tree ---
$gitStatus = & git status --porcelain 2>&1 | Out-String
if ($gitStatus.Trim()) {
    Write-Warning "Working tree has uncommitted changes:"
    Write-Host $gitStatus
    $confirm = Read-Host "Continue anyway? (y/N)"
    if ($confirm -ne "y") { exit 1 }
}

# --- Ensure tag doesn't already exist on remote ---
$existingTag = & git ls-remote --tags origin $tag 2>&1 | Out-String
if ($existingTag.Trim()) {
    Write-Error "Tag $tag already exists on origin."
    exit 1
}

# --- Run the packaging script ---
Write-Host "`n=== Packaging $tag ==="
& ./package.ps1
if ($LASTEXITCODE -ne 0) { Write-Error "package.ps1 failed."; exit 1 }

$zipPath = "dist/ParseBot.zip"
if (-not (Test-Path $zipPath)) {
    Write-Error "Expected $zipPath not produced by package.ps1"
    exit 1
}

# --- Create the GitHub release ---
Write-Host "`n=== Creating GitHub release $tag ==="

$releaseArgs = @("release", "create", $tag, $zipPath, "--title", $tag, "--target", "master")
if ($Prerelease) { $releaseArgs += "--prerelease" }
if ($NotesFile) {
    if (-not (Test-Path $NotesFile)) {
        Write-Error "Notes file not found: $NotesFile"
        exit 1
    }
    $releaseArgs += @("--notes-file", $NotesFile)
} elseif ($Notes) {
    $releaseArgs += @("--notes", $Notes)
} else {
    $releaseArgs += @("--generate-notes")
}

& gh @releaseArgs
if ($LASTEXITCODE -ne 0) { Write-Error "gh release create failed."; exit 1 }

Write-Host "`nRelease $tag created with asset $zipPath"
