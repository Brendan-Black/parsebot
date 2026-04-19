param(
  [Parameter(Mandatory=$true)][int]$AdminPid,
  [Parameter(Mandatory=$true)][string]$InstallDir,
  [Parameter(Mandatory=$true)][string]$StagedRoot,
  [Parameter(Mandatory=$true)][string]$ServiceName,
  [Parameter(Mandatory=$true)][string]$AdminExe,
  [Parameter(Mandatory=$true)][string]$LogFile
)

$ErrorActionPreference = "Continue"
Start-Transcript -Path $LogFile -Append | Out-Null

function Log($msg) {
  $stamp = Get-Date -Format "HH:mm:ss"
  Write-Host "[$stamp] $msg"
}

Log "ParseBot updater starting."
Log "AdminPid=$AdminPid"
Log "InstallDir=$InstallDir"
Log "StagedRoot=$StagedRoot"

# 1. Wait for admin to exit so its files become unlocked.
Log "Waiting for admin process $AdminPid to exit..."
$deadline = (Get-Date).AddSeconds(60)
while ((Get-Date) -lt $deadline) {
  $p = Get-Process -Id $AdminPid -ErrorAction SilentlyContinue
  if (-not $p) { break }
  Start-Sleep -Milliseconds 300
}
if (Get-Process -Id $AdminPid -ErrorAction SilentlyContinue) {
  Log "WARNING: admin process still alive after 60s; proceeding anyway."
}

# 2. Stop the Windows service.
Log "Stopping service $ServiceName..."
& sc.exe stop $ServiceName 2>&1 | ForEach-Object { Log "  $_" }
$deadline = (Get-Date).AddSeconds(30)
while ((Get-Date) -lt $deadline) {
  $q = (& sc.exe query $ServiceName 2>&1 | Out-String)
  if ($q -match "STOPPED") { break }
  if ($q -match "does not exist") { break }
  Start-Sleep -Milliseconds 500
}

# 3. Remove old app artifacts (preserve logs/, state/, anything else).
$toRemove = @("admin.exe", "service.exe", "installer.exe", "app", "runtime")
foreach ($item in $toRemove) {
  $p = Join-Path $InstallDir $item
  if (-not (Test-Path $p)) { continue }
  Log "Removing $p"
  $attempts = 0
  while ($attempts -lt 12) {
    try {
      Remove-Item -Recurse -Force -LiteralPath $p -ErrorAction Stop
      break
    } catch {
      $attempts++
      Log "  retry $attempts : $($_.Exception.Message)"
      Start-Sleep -Milliseconds 500
    }
  }
  if (Test-Path $p) {
    Log "ERROR: could not remove $p — aborting update."
    Stop-Transcript | Out-Null
    Read-Host "Press Enter to close"
    exit 1
  }
}

# 4. Copy staged files into install dir.
Log "Copying staged files from $StagedRoot to $InstallDir..."
Get-ChildItem -LiteralPath $StagedRoot -Force | ForEach-Object {
  $target = Join-Path $InstallDir $_.Name
  Log "  $($_.Name)"
  Copy-Item -Recurse -Force -LiteralPath $_.FullName -Destination $target
}

# 5. Start the service again.
Log "Starting service $ServiceName..."
& sc.exe start $ServiceName 2>&1 | ForEach-Object { Log "  $_" }

# 6. Launch the new admin.
$newAdmin = Join-Path $InstallDir $AdminExe
if (Test-Path $newAdmin) {
  Log "Launching $newAdmin"
  Start-Process -FilePath $newAdmin
} else {
  Log "WARNING: admin executable not found at $newAdmin"
}

Log "Update complete."
Stop-Transcript | Out-Null
