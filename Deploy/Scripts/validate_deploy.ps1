# EverTask Deploy Validation Script (PowerShell)
# Usage: .\validate_deploy.ps1

$ErrorActionPreference = "Stop"

$deployDir = Resolve-Path (Join-Path $PSScriptRoot "..")

$requiredFiles = @(
    "Android\Build\Output\evertask-1.0.0-release.aab",
    "Android\Build\Output\evertask-1.0.0-debug.apk",
    "Android\Build\Output\mapping\mapping.txt",
    "Android\StoreListing\title.txt",
    "Android\StoreListing\short_description.txt",
    "Android\StoreListing\full_description.txt",
    "Android\StoreListing\Icon\icon_512x512.png",
    "Android\StoreListing\FeatureGraphic\feature_graphic_1024x500.png",
    "Android\Policy\data_safety_form.md",
    "Android\Policy\data_safety_labels.json",
    "Android\Policy\content_rating_guide.md",
    "Android\Build\build_instructions.md",
    "Android\Build\keystore_setup.md",
    "Review\ReviewNotes.txt",
    "Review\DemoVideoScript.txt",
    "Review\ContactInfo.txt",
    "Review\TestAccounts.txt",
    "Review\SubmissionChecklist.txt",
    "Review\ReviewResponseTemplates.txt",
    "Shared\Legal\privacy_policy.md",
    "Shared\Legal\terms_of_service.md",
    "Shared\Legal\data_deletion_policy.md",
    "Shared\Legal\eula.md",
    "Marketing\press_release.md",
    "README.md",
    "CHECKLIST.md",
    "INDEX.md"
)

$issues = @()

Write-Host "========================================"
Write-Host " EverTask Deploy Validation"
Write-Host "========================================"

foreach ($relPath in $requiredFiles) {
    $fullPath = Join-Path $deployDir $relPath
    if (Test-Path $fullPath) {
        $size = (Get-Item $fullPath).Length
        Write-Host "[OK] $relPath ($size bytes)" -ForegroundColor Green
    } else {
        Write-Host "[MISSING] $relPath" -ForegroundColor Red
        $issues += $relPath
    }
}

# Size checks
$aabPath = Join-Path $deployDir "Android\Build\Output\evertask-1.0.0-release.aab"
if (Test-Path $aabPath) {
    $aabSize = (Get-Item $aabPath).Length
    if ($aabSize -lt 1MB) {
        Write-Host "[WARN] AAB is suspiciously small ($aabSize bytes)" -ForegroundColor Yellow
        $issues += "AAB size check"
    } else {
        Write-Host "[OK] AAB size: $([math]::Round($aabSize/1MB, 2)) MB" -ForegroundColor Green
    }
}

$apkPath = Join-Path $deployDir "Android\Build\Output\evertask-1.0.0-debug.apk"
if (Test-Path $apkPath) {
    $apkSize = (Get-Item $apkPath).Length
    if ($apkSize -lt 1MB) {
        Write-Host "[WARN] APK is suspiciously small ($apkSize bytes)" -ForegroundColor Yellow
        $issues += "APK size check"
    } else {
        Write-Host "[OK] APK size: $([math]::Round($apkSize/1MB, 2)) MB" -ForegroundColor Green
    }
}

# Image dimension checks
try {
    Add-Type -AssemblyName System.Drawing | Out-Null
    $iconPath = Join-Path $deployDir "Android\StoreListing\Icon\icon_512x512.png"
    if (Test-Path $iconPath) {
        $img = [System.Drawing.Image]::FromFile($iconPath)
        if ($img.Width -eq 512 -and $img.Height -eq 512) {
            Write-Host "[OK] Store icon dimensions: $($img.Width)x$($img.Height)" -ForegroundColor Green
        } else {
            Write-Host "[WARN] Store icon dimensions are $($img.Width)x$($img.Height), expected 512x512" -ForegroundColor Yellow
            $issues += "Icon dimension check"
        }
        $img.Dispose()
    }

    $fgPath = Join-Path $deployDir "Android\StoreListing\FeatureGraphic\feature_graphic_1024x500.png"
    if (Test-Path $fgPath) {
        $img = [System.Drawing.Image]::FromFile($fgPath)
        if ($img.Width -eq 1024 -and $img.Height -eq 500) {
            Write-Host "[OK] Feature graphic dimensions: $($img.Width)x$($img.Height)" -ForegroundColor Green
        } else {
            Write-Host "[WARN] Feature graphic dimensions are $($img.Width)x$($img.Height), expected 1024x500" -ForegroundColor Yellow
            $issues += "Feature graphic dimension check"
        }
        $img.Dispose()
    }
} catch {
    Write-Host "[INFO] Image dimension checks skipped (System.Drawing unavailable)" -ForegroundColor Cyan
}

Write-Host ""
if ($issues.Count -eq 0) {
    Write-Host "========================================" -ForegroundColor Green
    Write-Host " ✅ DEPLOY PACKAGE VALID" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    exit 0
} else {
    Write-Host "========================================" -ForegroundColor Red
    Write-Host " ❌ DEPLOY PACKAGE INVALID" -ForegroundColor Red
    Write-Host " Issues found: $($issues.Count)" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    foreach ($issue in $issues) {
        Write-Host "  - $issue" -ForegroundColor Red
    }
    exit 1
}
