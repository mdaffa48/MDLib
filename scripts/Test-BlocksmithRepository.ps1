[CmdletBinding()]
param([switch]$SaveCredentials)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path $PSScriptRoot -Parent
$settingsPath = Join-Path $repositoryRoot 'settings.xml'
$resultPath = Join-Path $repositoryRoot 'target\blocksmith-connection-result.json'
New-Item -ItemType Directory -Force -Path (Split-Path $resultPath -Parent) | Out-Null
Remove-Item Env:BLOCKSMITH_REPO_TOKEN_NAME, Env:BLOCKSMITH_REPO_TOKEN_VALUE -ErrorAction SilentlyContinue

$result = [ordered]@{
    endpoint = 'https://repo.blocksmith.studio/api/auth/me'
    status = 'AwaitingCredentials'
    httpStatus = $null
    authenticated = $false
    published = $false
    settingsSaved = $false
}
$result | ConvertTo-Json | Set-Content -LiteralPath $resultPath -Encoding UTF8
$client = $null
$handler = $null
$request = $null
$response = $null
$credentialBytes = $null
$tokenValue = $null

try {
    Write-Host 'Read-only Blocksmith connection test. Nothing will be published.'
    if (-not $SaveCredentials -and (Test-Path -LiteralPath $settingsPath)) {
        $savedSettings = [xml][IO.File]::ReadAllText($settingsPath)
        $savedServer = $savedSettings.SelectSingleNode("//*[local-name()='server'][*[local-name()='id']='blocksmith']")
        if (-not $savedServer) {
            $result.status = 'MissingSavedServer'
            throw 'Missing saved server'
        }
        $tokenName = [string]$savedServer.username
        $tokenValue = ConvertTo-SecureString ([string]$savedServer.password) -AsPlainText -Force
        $result.settingsSaved = $true
        Write-Host 'Testing credentials from the local settings.xml file.'
    } else {
        if ($SaveCredentials) {
            Write-Host 'After successful authentication, credentials will be saved to a Git-ignored settings.xml restricted to your Windows account.'
        } else {
            Write-Host 'The value is masked and is not saved to disk.'
        }
        Write-Host 'Enter the token here, not in chat.'
        $tokenName = Read-Host 'Token name'
        $tokenValue = Read-Host 'Token value' -AsSecureString
    }
    if ([string]::IsNullOrWhiteSpace($tokenName) -or $tokenName.Contains(':') -or $tokenValue.Length -eq 0) {
        $result.status = 'InvalidCredentialsInput'
        throw 'Invalid input'
    }

    Add-Type -AssemblyName System.Net.Http
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $credential = [PSCredential]::new($tokenName, $tokenValue)
    $credentialBytes = [Text.Encoding]::UTF8.GetBytes($tokenName + ':' + $credential.GetNetworkCredential().Password)
    $handler = [Net.Http.HttpClientHandler]::new()
    $handler.AllowAutoRedirect = $false
    $handler.UseCookies = $false
    $client = [Net.Http.HttpClient]::new($handler)
    $client.Timeout = [TimeSpan]::FromSeconds(20)
    $request = [Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::Get, $result.endpoint)
    $request.Headers.Authorization = [Net.Http.Headers.AuthenticationHeaderValue]::new('Basic', [Convert]::ToBase64String($credentialBytes))
    $request.Headers.Accept.ParseAdd('application/json')
    $result.status = 'Checking'
    $result | ConvertTo-Json | Set-Content -LiteralPath $resultPath -Encoding UTF8
    $response = $client.SendAsync($request).GetAwaiter().GetResult()
    $result.httpStatus = [int]$response.StatusCode

    if ($result.httpStatus -eq 200) {
        if ($response.Content.Headers.ContentType.MediaType -ne 'application/json') {
            $result.status = 'UnexpectedResponse'
            throw 'Unexpected response'
        }
        $session = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
        foreach ($field in 'accessToken', 'permissions', 'routes') {
            if ($session.PSObject.Properties.Name -notcontains $field) {
                $result.status = 'UnexpectedResponse'
                throw 'Unexpected response'
            }
        }
        $result.authenticated = $true
        $env:BLOCKSMITH_REPO_TOKEN_NAME = $tokenName
        $env:BLOCKSMITH_REPO_TOKEN_VALUE = $credential.GetNetworkCredential().Password
        if ($SaveCredentials) {
            $result.status = 'CredentialsSaveFailed'
            $sourceSettingsPath = Join-Path $repositoryRoot 'settings-publish.xml'
            if (Test-Path -LiteralPath $settingsPath) {
                $sourceSettingsPath = $settingsPath
            }
            $localSettings = [xml][IO.File]::ReadAllText($sourceSettingsPath)
            $localServer = $localSettings.SelectSingleNode("//*[local-name()='server'][*[local-name()='id']='blocksmith']")
            if (-not $localServer) { throw 'Missing local server' }
            $localServer.username = $tokenName
            $localServer.password = $credential.GetNetworkCredential().Password
            if (-not (Test-Path -LiteralPath $settingsPath)) {
                [IO.File]::Create($settingsPath).Dispose()
            }
            $currentUser = [Security.Principal.WindowsIdentity]::GetCurrent().User
            $fileSecurity = [Security.AccessControl.FileSecurity]::new()
            $fileSecurity.SetOwner($currentUser)
            $fileSecurity.SetAccessRuleProtection($true, $false)
            $fileSecurity.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($currentUser, 'FullControl', 'Allow'))
            Set-Acl -LiteralPath $settingsPath -AclObject $fileSecurity
            $localSettings.Save($settingsPath)
            $mavenConfigPath = Join-Path $repositoryRoot '.mvn\maven.config'
            if (-not (Test-Path -LiteralPath $mavenConfigPath)) {
                [IO.File]::WriteAllLines($mavenConfigPath, @('--settings', $settingsPath), [Text.UTF8Encoding]::new($false))
            }
            $result.settingsSaved = $true
        }
        $result.status = 'Authenticated'
        Write-Host 'Authentication succeeded (HTTP 200). No artifacts were uploaded.' -ForegroundColor Green
        if ($result.settingsSaved) {
            Write-Host 'Credentials are saved in the protected local settings.xml. Maven is configured to use it automatically.'
            Write-Host 'This window can be closed. Publishing permission has not been tested.'
        } else {
            Write-Host 'Credentials are available only in this PowerShell window for later Maven commands.'
            Write-Host 'Close this window to discard them. Publishing permission has not been tested.'
        }
    } else {
        $result.status = 'AuthenticationRejected'
        Write-Host ('Authentication did not succeed (HTTP {0}). No artifacts were uploaded.' -f $result.httpStatus) -ForegroundColor Yellow
    }
} catch {
    $result.errorType = $_.Exception.GetType().Name
    if ($result.status -in 'AwaitingCredentials', 'Checking') {
        $result.status = 'ConnectionError'
    }
    Write-Host ('Test did not complete: {0}. No credentials or server response details were logged.' -f $result.status) -ForegroundColor Yellow
} finally {
    if ($response) { $response.Dispose() }
    if ($request) { $request.Dispose() }
    if ($client) { $client.Dispose() }
    if ($handler) { $handler.Dispose() }
    if ($credentialBytes) { [Array]::Clear($credentialBytes, 0, $credentialBytes.Length) }
    if ($tokenValue) { $tokenValue.Dispose() }
    $result | ConvertTo-Json | Set-Content -LiteralPath $resultPath -Encoding UTF8
}
