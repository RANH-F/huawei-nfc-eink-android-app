# NFC system profile collector for SN100U / QTag debugging.
# Read-only. No root, Shizuku, proprietary NCI commands or NFC setting changes.
# Windows PowerShell 5.1+; install Android Platform Tools and enable USB debugging.
param([string]$Adb = "adb")

$ErrorActionPreference = "Stop"
try {
    & $Adb version | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "adb command unavailable" }
    $devices = @(& $Adb devices)
    if (-not (($devices | Select-String -Pattern "\tdevice$").Count -eq 1)) {
        throw "Expected exactly one authorized ADB device. Run adb devices first."
    }
} catch {
    Write-Host "ADB not ready: $_" -ForegroundColor Red
    exit 1
}

$when = Get-Date -Format "yyyyMMdd-HHmmss"
$destination = Join-Path (Get-Location) ("nfc-profile-" + $when + ".txt")
$lines = New-Object 'System.Collections.Generic.List[string]'
$lines.Add("Ink NFC Lab / QPollLab - SN100U read-only profile")
$lines.Add("Collected: " + (Get-Date -Format s))
$lines.Add("NOTE: no custom NCI command is sent and no NFC setting is changed.")
$lines.Add("Share only after manually reviewing and redacting personal identifiers.")
$lines.Add("")

function Capture([string] $title, [string] $shellCommand) {
    $lines.Add("=== " + $title + " ===")
    $text = & $Adb shell $shellCommand 2>&1
    foreach ($item in @($text)) { if ($null -ne $item) { $lines.Add([string]$item) } }
    $lines.Add("")
}

# Avoid full dumpsys nfc or logcat: they may contain nearby card UIDs and payment data.
Capture "Device model" "getprop ro.product.model"
Capture "Device codename" "getprop ro.product.device"
Capture "Android API" "getprop ro.build.version.sdk"
Capture "Android release" "getprop ro.build.version.release"
Capture "NFC hardware" "getprop ro.hardware.nfc_nci"
Capture "Vendor NFC HAL" "getprop ro.vendor.nfc_nci"
Capture "NFC controller property" "getprop ro.vendor.nfc.chip"
Capture "NFC firmware config path" "ls -l /vendor/etc/libnfc-nxp.conf"
Capture "NFC system config path" "ls -l /system/etc/libnfc-nci.conf"
Capture "NFC package" "pm path com.android.nfc"
Capture "NXP NFC package" "pm path com.nxp.nfc"
Capture "NFC services (names only)" "service list | grep -i nfc"
Capture "NFC HAL shared objects (filenames only)" "ls /vendor/lib64/hw | grep -i nfc"
Capture "NFC firmware names (filenames only)" "ls /vendor/firmware | grep -Ei 'nfc|sn100|nxp'"

$lines.Add("NCI implementation note:")
$lines.Add("QPollLab v2.9 qtag() submits (mt=1, gid=15, oid=112=0x70, payload=31 01/00).")
$lines.Add("Standard NCI OID mask is 0x3F. Verify the intended OEM-specific wire protocol and HAL before changing the command.")
$lines.Add("A successful IPC method lookup or Shizuku UID=2000 does not prove vendor NCI support.")

$lines | Out-File -LiteralPath $destination -Encoding UTF8
Write-Host "Read-only report saved to: $destination" -ForegroundColor Green
Write-Host "Please inspect the file and redact personal data before sharing."
