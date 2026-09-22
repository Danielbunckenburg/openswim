param([string]$Avd = 'OpenSwim_WearOS')
$ErrorActionPreference = 'Stop'
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$java = $env:JAVA_HOME
if (-not $java) { $java = 'C:\Program Files\Android\Android Studio\jbr' }
if (-not (Test-Path (Join-Path $sdk 'platform-tools\adb.exe'))) { throw "Android SDK not found: $sdk" }
if (-not (Test-Path (Join-Path $java 'bin\java.exe'))) { throw "JDK not found: $java" }
$env:ANDROID_HOME = $sdk
$env:JAVA_HOME = $java
if (-not $env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME = Join-Path $project '.gradle-user' }
if (-not $env:ANDROID_USER_HOME) { $env:ANDROID_USER_HOME = Join-Path $project '.android' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$emulator = Join-Path $sdk 'emulator\emulator.exe'

function Get-AvdSerial {
    foreach ($line in (& $adb devices)) {
        if ($line -match '^(emulator-\d+)\s+device\b') {
            $candidate = $Matches[1]
            $name = (& $adb -s $candidate emu avd name 2>$null) -join ' '
            if ($name -match [regex]::Escape($Avd)) { return $candidate }
        }
    }
    return $null
}

$serial = Get-AvdSerial
if (-not $serial) {
    if (-not ((& $emulator -list-avds) -contains $Avd)) { throw "AVD not found: $Avd" }
    Start-Process -FilePath $emulator -ArgumentList @('-avd', $Avd) -WindowStyle Hidden | Out-Null
    $deadline = (Get-Date).AddMinutes(5)
    do { Start-Sleep -Seconds 3; $serial = Get-AvdSerial } until ($serial -or (Get-Date) -ge $deadline)
    if (-not $serial) { throw "AVD $Avd did not appear in adb" }
}
$deadline = (Get-Date).AddMinutes(5)
do { $boot = & $adb -s $serial shell getprop sys.boot_completed; if ($boot -eq '1') { break }; Start-Sleep -Seconds 3 } until ((Get-Date) -ge $deadline)
if ($boot -ne '1') { throw 'Emulator did not finish booting' }
Push-Location $project
try { & .\gradlew.bat :wear:assembleDebug --console=plain; if ($LASTEXITCODE -ne 0) { throw 'Build failed' } } finally { Pop-Location }
& $adb -s $serial install -r (Join-Path $project 'wear\build\outputs\apk\debug\wear-debug.apk')
if ($LASTEXITCODE -ne 0) { throw 'Install failed' }
& $adb -s $serial shell am start -n 'org.openswim.wear/.MainActivity'
if ($LASTEXITCODE -ne 0) { throw 'Launch failed' }
Write-Host "OpenSwim launched on $Avd ($serial)"
