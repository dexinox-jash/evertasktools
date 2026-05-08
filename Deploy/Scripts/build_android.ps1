# EverTask Android Build Script (PowerShell)
# Usage: .\build_android.ps1

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$kmpDir = Join-Path $repoRoot "kmp"
$outputDir = Join-Path $repoRoot "Deploy" "Android" "Build" "Output"

Write-Host "========================================"
Write-Host " EverTask Android Build Script"
Write-Host "========================================"

# Verify gradlew exists
$gradlew = Join-Path $kmpDir "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    Write-Error "gradlew.bat not found at $gradlew"
    exit 1
}

Set-Location $kmpDir

# Run tests
Write-Host "`n[1/4] Running unit tests..." -ForegroundColor Cyan
& $gradlew ":androidApp:testDebugUnitTest" --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Error "androidApp unit tests failed. Fix before building release."
    exit 1
}

& $gradlew ":shared:testDebugUnitTest" --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Error "shared unit tests failed. Fix before building release."
    exit 1
}

# Build release AAB
Write-Host "`n[2/4] Building release AAB..." -ForegroundColor Cyan
& $gradlew ":androidApp:bundleRelease" --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Error "Release AAB build failed."
    exit 1
}

# Copy outputs
Write-Host "`n[3/4] Copying outputs to Deploy folder..." -ForegroundColor Cyan
$aabSource = Join-Path $kmpDir "androidApp" "build" "outputs" "bundle" "release" "androidApp-release.aab"
$apkSource = Join-Path $kmpDir "androidApp" "build" "outputs" "apk" "debug" "androidApp-debug.apk"
$mappingSource = Join-Path $kmpDir "androidApp" "build" "outputs" "mapping" "release"

New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $outputDir "mapping") -Force | Out-Null

Copy-Item $aabSource (Join-Path $outputDir "evertask-1.0.0-release.aab") -Force
Write-Host "  Copied release AAB"

if (Test-Path $apkSource) {
    Copy-Item $apkSource (Join-Path $outputDir "evertask-1.0.0-debug.apk") -Force
    Write-Host "  Copied debug APK"
}

if (Test-Path $mappingSource) {
    Copy-Item (Join-Path $mappingSource "*") (Join-Path $outputDir "mapping") -Force
    Write-Host "  Copied ProGuard mapping files"
}

# Validate
Write-Host "`n[4/4] Running deploy validation..." -ForegroundColor Cyan
$validator = Join-Path $repoRoot "Deploy" "Scripts" "validate_deploy.ps1"
& $validator

Write-Host "`n========================================" -ForegroundColor Green
Write-Host " Build completed successfully!" -ForegroundColor Green
Write-Host "========================================"
