$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (!(Test-Path '.env')) {
    function New-Secret([int]$Length) {
        $bytes = New-Object byte[] $Length
        $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
        return [Convert]::ToBase64String($bytes)
    }
    $db = New-Secret 24
    $jwt = New-Secret 48
    $admin = New-Secret 18
    @("DB_PASSWORD=$db", "JWT_SECRET=$jwt", 'ADMIN_LOCAL_PART=admin', "ADMIN_PASSWORD=$admin") | Set-Content -Encoding ascii '.env'
    Write-Host 'Created local configuration.'
}
Write-Host 'Initial Admin credentials (used only when the database is empty):'
Write-Host 'Read your initial Admin credentials privately from the ignored .env file. They are not printed.'
