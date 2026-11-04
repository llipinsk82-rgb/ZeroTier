param(
    [Parameter(Mandatory = $true)]
    [string]$Destination
)

$ErrorActionPreference = 'Stop'
$upstream = 'https://github.com/kaaass/ZerotierFix.git'
$pinnedSha = '0373a58c03ffe053e92212a6fa2d98245db8aa9c'
$patchNames = @(
    '0001-blackserv-zerotier-tv.patch',
    '0002-disable-ipv6-for-explicit-ipv4-exit.patch',
    '0003-verify-pinned-public-ipv4-exit.patch',
    '0004-include-zerotier-java-sdk-sources.patch',
    '0005-full-tv-client.patch',
    '0006-fix-deleted-tuner-cards.patch'
)
$patches = @($patchNames | ForEach-Object { Join-Path $PSScriptRoot ('..\patches\' + $_) })

if (Test-Path -LiteralPath $Destination) {
    throw "Destination already exists. Nothing overwritten: $Destination"
}
foreach ($patch in $patches) {
    if (-not (Test-Path -LiteralPath $patch)) {
        throw "Missing project patch: $patch"
    }
}
git clone --no-checkout $upstream $Destination
if ($LASTEXITCODE -ne 0) { throw 'Clone failed' }
git -C $Destination checkout --detach $pinnedSha
if ($LASTEXITCODE -ne 0) { throw 'Pinned upstream checkout failed' }
git -C $Destination submodule update --init --depth 1
if ($LASTEXITCODE -ne 0) { throw 'ZeroTier core checkout failed' }
foreach ($patch in $patches) {
    git -C $Destination apply --check $patch
    if ($LASTEXITCODE -ne 0) { throw "Patch incompatible with upstream: $patch" }
    git -C $Destination apply $patch
    if ($LASTEXITCODE -ne 0) { throw "Cannot apply patch: $patch" }
}
Write-Host "BlackServ ZeroTier TV source prepared: $Destination"
Write-Host 'No APK was built or installed. Android SDK + NDK and license acceptance are required.'
