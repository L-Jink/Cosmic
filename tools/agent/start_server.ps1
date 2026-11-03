#Requires -Version 5.1
<#
.SYNOPSIS
  One command: build and start the Cosmic server from the current folder, a given folder, or a PR, so you can log in and test.

.PARAMETER Pr
  PR number on L-Jink/Cosmic. Finds its branch with gh and uses (or creates) .worktrees\pr<number>.

.PARAMETER Dir
  Use this folder (a checkout/worktree of Cosmic) instead of the current one.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools/agent/start_server.ps1 -Pr 7
.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools/agent/start_server.ps1 -Dir C:\Users\joshs\Documents\GitHub\Cosmic\.worktrees\t_47e349e3
.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools/agent/start_server.ps1

  Press Ctrl+C in the window to stop the server. Uses config.local.yaml (git-ignored) for the DB password; never printed.
#>
param(
    [int]$Pr = 0,
    [string]$Dir = ""
)
$ErrorActionPreference = "Stop"
$Repo = "L-Jink/Cosmic"
$Port = 8484

function Fail($msg) { Write-Host ""; Write-Host "ERROR: $msg" -ForegroundColor Red; exit 1 }

# 1. Refuse if a server is already up (checked first so nothing slow happens).
$busy = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($busy) {
    Fail "Port $Port is already in use (process id $($busy.OwningProcess)) - a Cosmic server is probably already running. Stop it first (Ctrl+C in its window, or: Stop-Process -Id $($busy.OwningProcess))."
}

# 2. Pick the folder.
if ($Pr -gt 0 -and $Dir) { Fail "Use either -Pr or -Dir, not both." }
$startDir = (Get-Location).Path
if ($Dir) {
    if (-not (Test-Path $Dir -PathType Container)) { Fail "Folder not found: $Dir" }
    $startDir = (Resolve-Path $Dir).Path
}
Set-Location $startDir
$top = (& git rev-parse --show-toplevel 2>$null)
if ($LASTEXITCODE -ne 0 -or -not $top) { Fail "$startDir is not inside a git checkout of Cosmic." }
$repoRoot = $top.Trim()
$common = (& git rev-parse --path-format=absolute --git-common-dir).Trim()
$mainRoot = Split-Path $common -Parent   # the main checkout (holds config.local.yaml)

if ($Pr -gt 0) {
    if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { Fail "GitHub CLI (gh) not found; cannot look up PR #$Pr. Use -Dir instead." }
    $branch = (& gh pr view $Pr -R $Repo --json headRefName -q .headRefName 2>$null)
    if ($LASTEXITCODE -ne 0 -or -not $branch) { Fail "Could not find PR #$Pr on $Repo (is gh logged in? run: gh auth status)." }
    $branch = $branch.Trim()
    $wt = Join-Path $mainRoot ".worktrees\pr$Pr"
    Write-Host "[start] PR #$Pr -> branch $branch -> $wt"
    & git -C $mainRoot fetch origin $branch 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { Fail "git fetch of branch '$branch' failed." }
    if (Test-Path $wt) {
        $dirty = (& git -C $wt status --porcelain)
        if ($dirty) { Fail "$wt has uncommitted changes; refusing to overwrite. Clean it or remove the folder." }
        & git -C $wt checkout --detach FETCH_HEAD 2>&1 | Out-Null
    } else {
        & git -C $mainRoot worktree add --detach $wt FETCH_HEAD 2>&1 | Out-Null
    }
    if ($LASTEXITCODE -ne 0) { Fail "Could not prepare worktree $wt." }
    $repoRoot = $wt
}
Set-Location $repoRoot
Write-Host "[start] folder: $repoRoot"
Write-Host "[start] branch: $((& git rev-parse --abbrev-ref HEAD).Trim()) @ $((& git rev-parse --short HEAD).Trim())"

# 3. Java + local config (DB password lives in git-ignored config.local.yaml; the path is shown, the contents never are).
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $jdk = Get-ChildItem "C:\Program Files\Java\jdk-*" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}
if ($env:JAVA_HOME) { $env:PATH = (Join-Path $env:JAVA_HOME "bin") + ";" + $env:PATH }
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { Fail "Java not found. Install JDK 21 or set JAVA_HOME." }
if (-not $env:COSMIC_LOCAL_CONFIG) {
    foreach ($c in @((Join-Path $repoRoot "config.local.yaml"), (Join-Path $mainRoot "config.local.yaml"))) {
        if (Test-Path $c -PathType Leaf) { $env:COSMIC_LOCAL_CONFIG = $c; break }
    }
}
if ($env:COSMIC_LOCAL_CONFIG) { Write-Host "[start] using local config: $env:COSMIC_LOCAL_CONFIG" }
else { Write-Host "[start] no config.local.yaml found - using config.yaml defaults (empty DB password)" }

# 4. Build (skip tests).
Write-Host "[start] building (mvnw package -DskipTests) - this takes a minute ..."
& .\mvnw.cmd -q -B package -DskipTests
if ($LASTEXITCODE -ne 0) { Fail "Build failed (exit $LASTEXITCODE). Scroll up for the compiler error." }
$jar = Join-Path $repoRoot "target\Cosmic.jar"
if (-not (Test-Path $jar)) { Fail "Build finished but $jar is missing." }

# Re-check the port: the build takes a while.
$busy = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($busy) { Fail "Port $Port got taken during the build (process id $($busy.OwningProcess)). Not starting." }

# 5. Run in the foreground (output is shown here); Ctrl+C stops it.
Write-Host "[start] starting server ... (Ctrl+C to stop)"
$proc = Start-Process -FilePath "java" -ArgumentList @("-Xmx2048m", "-Dwz-path=wz", "-jar", "target\Cosmic.jar") `
    -WorkingDirectory $repoRoot -NoNewWindow -PassThru
$ready = $false
try {
    while (-not $proc.HasExited) {
        if (-not $ready -and (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)) {
            $ready = $true
            Write-Host ""
            Write-Host "=== SERVER READY - log in now ===  (login port $Port; Ctrl+C here to stop)" -ForegroundColor Green
            Write-Host ""
        }
        Start-Sleep -Milliseconds 500
    }
    if (-not $ready) { Write-Host "ERROR: server exited (code $($proc.ExitCode)) before opening port $Port." -ForegroundColor Red; exit 1 }
    Write-Host "[start] server exited (code $($proc.ExitCode))."
} finally {
    if ($proc -and -not $proc.HasExited) {
        Write-Host ""
        Write-Host "[start] stopping server ..."
        $proc.Kill()
        $proc.WaitForExit(15000) | Out-Null
        Write-Host "[start] server stopped."
    }
}
