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
        Name      = "installer"
        MainClass = "black.parsebot.installer.Main"
        MainJar   = "installer-0.1.0.jar"
    }
)

# --- Resolve JDK path via Gradle toolchain ---
Write-Host "Resolving JDK 25 toolchain path..."
$toolchainOutput = & ./gradlew -q javaToolchains 2>&1 | Out-String
$jdkPath = ($toolchainOutput |
    Select-String -Pattern "^\s+\|.*Location:\s+(.+)$" -AllMatches).Matches |
    ForEach-Object { $_.Groups[1].Value.Trim() } |
    Where-Object { Test-Path (Join-Path $_ "bin/jpackage.exe") } |
    Select-Object -First 1

if (-not $jdkPath) {
    Write-Error "Could not locate a JDK with jpackage via Gradle toolchains."
    exit 1
}

$jpackage = Join-Path $jdkPath "bin/jpackage.exe"
Write-Host "Using jpackage: $jpackage"

# --- Build all subprojects ---
Write-Host "`nBuilding project..."
& ./gradlew clean build
if ($LASTEXITCODE -ne 0) { Write-Error "Gradle build failed."; exit 1 }

# --- Prepare output directory ---
if (Test-Path $OutputDir) { Remove-Item -Recurse -Force $OutputDir }
New-Item -ItemType Directory -Path $OutputDir | Out-Null

# --- Package each subproject ---
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
    & ./gradlew ":${name}:copyDependencies" 2>$null
    if (($LASTEXITCODE -eq 0) -and (Test-Path $depsDir)) {
        Copy-Item "$depsDir/*.jar" $inputDir -ErrorAction SilentlyContinue
    }

    $jpackageArgs = @(
        "--type",       "app-image"
        "--name",       $name
        "--input",      $inputDir
        "--main-jar",   $proj.MainJar
        "--main-class", $proj.MainClass
        "--dest",       $OutputDir
    )

    Write-Host "Running: jpackage $($jpackageArgs -join ' ')"
    & $jpackage @jpackageArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Error "jpackage failed for '$name'."
        exit 1
    }

    Write-Host "Packaged $name -> $OutputDir/$name/"
}

Write-Host "`nDone. Executables are in '$OutputDir/'."
