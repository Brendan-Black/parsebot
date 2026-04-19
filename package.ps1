param(
    [string]$OutputDir = "dist"
)

$ErrorActionPreference = "Stop"

$subprojects = @(
    @{
        Name      = "service"
        MainClass = "black.parsebot.Main"
        MainJar   = "service-0.1.0.jar"
    },
    @{
        Name      = "admin"
        MainClass = "black.parsebot.admin.Main"
        MainJar   = "admin-0.1.0.jar"
    }
)

# --- Resolve JDK path via Gradle toolchain ---
Write-Host "Resolving JDK 25 toolchain path..."
$ErrorActionPreference = "Continue"
$toolchainOutput = & ./gradlew -q javaToolchains 2>&1 | Out-String
$ErrorActionPreference = "Stop"
$lines = $toolchainOutput -split "`r?`n"
$jdkPath = $null
$inJdk25Block = $false
foreach ($line in $lines) {
    if ($line -match "JDK 25") { $inJdk25Block = $true }
    elseif ($line -match "^\s*$" -or $line -match "^\s+\+") { $inJdk25Block = $false }
    if ($inJdk25Block -and $line -match "^\s+\|\s+Location:\s+(.+)$") {
        $candidate = $Matches[1].Trim()
        if (Test-Path (Join-Path $candidate "bin/jpackage.exe")) {
            $jdkPath = $candidate
            break
        }
    }
}

if (-not $jdkPath) {
    Write-Error "Could not locate a JDK with jpackage via Gradle toolchains."
    exit 1
}

$jpackage = Join-Path $jdkPath "bin/jpackage.exe"
Write-Host "Using jpackage: $jpackage"

# --- Build all subprojects ---
Write-Host "`nBuilding project..."
$ErrorActionPreference = "Continue"
& ./gradlew clean build
$ErrorActionPreference = "Stop"
if ($LASTEXITCODE -ne 0) { Write-Error "Gradle build failed."; exit 1 }

# --- Prepare output directories ---
$stageDir = "$OutputDir/ParseBot"
if (Test-Path $OutputDir) { Remove-Item -Recurse -Force $OutputDir }
New-Item -ItemType Directory -Path $stageDir | Out-Null

# --- Package each subproject into a temporary location, then merge ---
$tempDir = "build/packaging/images"
if (Test-Path $tempDir) { Remove-Item -Recurse -Force $tempDir }

foreach ($proj in $subprojects) {
    $name    = $proj.Name
    $libsDir = "$name/build/libs"
    $mainJar = Join-Path $libsDir $proj.MainJar

    Write-Host "`n--- Packaging $name ---"

    # Collect runtime dependency jars into a staging directory
    $inputDir = "build/packaging/$name"
    if (Test-Path $inputDir) { Remove-Item -Recurse -Force $inputDir }
    New-Item -ItemType Directory -Path $inputDir | Out-Null

    Copy-Item $mainJar $inputDir

    $depsDir = "$name/build/dependencies"
    if (Test-Path $depsDir) { Remove-Item -Recurse -Force $depsDir }
    $ErrorActionPreference = "Continue"
    & ./gradlew ":${name}:copyDependencies" 2>$null
    $ErrorActionPreference = "Stop"
    if (($LASTEXITCODE -eq 0) -and (Test-Path $depsDir)) {
        Copy-Item "$depsDir/*.jar" $inputDir -ErrorAction SilentlyContinue
    }

    $jpackageArgs = @(
        "--type",        "app-image"
        "--name",        $name
        "--input",       $inputDir
        "--main-jar",    $proj.MainJar
        "--main-class",  $proj.MainClass
        "--dest",        $tempDir
        "--win-console"
    )

    Write-Host "Running: jpackage $($jpackageArgs -join ' ')"
    & $jpackage @jpackageArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Error "jpackage failed for '$name'."
        exit 1
    }

    Write-Host "Packaged $name"
}

# --- Merge both app-images into a single flat folder ---
Write-Host "`nMerging into single distribution folder..."

# Use the service image as the base (it has the larger runtime)
$serviceImage = "$tempDir/service"
Copy-Item -Recurse "$serviceImage/*" $stageDir

# Copy the admin executable alongside the service executable
$adminExe = "$tempDir/admin/admin.exe"
if (Test-Path $adminExe) {
    Copy-Item $adminExe $stageDir
}

# Copy any admin-only JARs into the app directory
$adminAppDir = "$tempDir/admin/app"
if (Test-Path $adminAppDir) {
    Get-ChildItem "$adminAppDir/*.jar" | ForEach-Object {
        $dest = Join-Path "$stageDir/app" $_.Name
        if (-not (Test-Path $dest)) {
            Copy-Item $_.FullName $dest
        }
    }
    # Copy admin .cfg so the exe can find its main class
    Get-ChildItem "$adminAppDir/*.cfg" | ForEach-Object {
        Copy-Item $_.FullName (Join-Path "$stageDir/app" $_.Name)
    }
}

# --- Create zip archive ---
$zipPath = "$OutputDir/ParseBot.zip"
Write-Host "`nCreating archive: $zipPath"
Compress-Archive -Path $stageDir -DestinationPath $zipPath -Force

Write-Host "`nDone."
Write-Host "  Folder: $stageDir/"
Write-Host "  Zip:    $zipPath"
