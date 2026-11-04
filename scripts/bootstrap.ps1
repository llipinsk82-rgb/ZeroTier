param(
    [Parameter(Mandatory = $true)]
    [string]$Destination
)

$ErrorActionPreference = 'Stop'
$upstream = 'https://github.com/kaaass/ZerotierFix.git'
$pinnedSha = '0373a58c03ffe053e92212a6fa2d98245db8aa9c'
$patch = Join-Path $PSScriptRoot '..\patches\0001-blackserv-zerotier-tv.patch'

if (Test-Path -LiteralPath $Destination) {
    throw "Destination already exists. Nothing overwritten: $Destination"
}
if (-not (Test-Path -LiteralPath $patch)) {
    throw "Missing project patch: $patch"
}
git clone --no-checkout $upstream $Destination
if ($LASTEXITCODE -ne 0) { throw 'Clone failed' }
git -C $Destination checkout --detach $pinnedSha
if ($LASTEXITCODE -ne 0) { throw 'Pinned upstream checkout failed' }
git -C $Destination submodule update --init --depth 1
if ($LASTEXITCODE -ne 0) { throw 'ZeroTier core checkout failed' }
git -C $Destination apply --check $patch
if ($LASTEXITCODE -ne 0) { throw 'Patch incompatible with pinned upstream' }
git -C $Destination apply $patch
if ($LASTEXITCODE -ne 0) { throw 'Patch could not be applied' }

Write-Host "BlackServ ZeroTier TV source prepared: $Destination"
Write-Host 'No APK was built or installed. Android SDK + NDK and license acceptance are required.'
