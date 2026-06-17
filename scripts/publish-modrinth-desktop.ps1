<#
  JustQuests -> Modrinth publisher (Desktop)

  Saves your Modrinth token ONCE (encrypted, Windows DPAPI: only you on
  this PC can read it), then uploads a new version each time you run it.

  FIRST-TIME SETUP
    1) Create a token at https://modrinth.com/settings/pats
       with scopes: "Create versions" + "Read projects"
       (and "Write projects" only if you'll use -SetEnvironment).
    2) Save it once:
         .\justquests-publish.ps1 -SaveToken
    3) (optional, once) set the project environment:
         .\justquests-publish.ps1 -SetEnvironment

  EVERY NEW RELEASE
    Build the jar, then:
         .\justquests-publish.ps1                 # uploads as "release"
         .\justquests-publish.ps1 -VersionType beta
#>

param(
    [switch]$SaveToken,
    [switch]$SetEnvironment,
    [ValidateSet("release","beta","alpha")]
    [string]$VersionType = "release",
    [string]$ProjectSlug = "justquests"
)

$ErrorActionPreference = "Stop"

# === editable settings ===
$RepoRoot     = "C:\Users\mukse\JustQuests"   # where the mod project lives
$GameVersions = @("1.21.1")                    # add MC versions when supported
$Loaders      = @("neoforge")                  # add "fabric"/"forge" after porting
# Modrinth has no "singleplayer" value - environment is client_side /
# server_side, each: required | optional | unsupported.
# JustQuests runs wherever the world is simulated (integrated server in
# singleplayer, the server on a dedicated server), so "required" on both
# is the honest setting. Change if you prefer.
$ClientSide   = "required"
$ServerSide   = "required"
$UserAgent    = "ErikEdits-JustQuests-publisher"
$TokenFile    = Join-Path $env:USERPROFILE ".justquests\modrinth-token.xml"
# =========================

function Save-Token {
    $dir = Split-Path $TokenFile -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    $sec = Read-Host "Paste your Modrinth token (hidden)" -AsSecureString
    $sec | Export-Clixml -Path $TokenFile     # DPAPI-encrypted, per-user
    Write-Host "Token saved (encrypted) to $TokenFile" -ForegroundColor Green
}

function Get-Token {
    if (-not (Test-Path $TokenFile)) {
        throw "No saved token. Run:  .\justquests-publish.ps1 -SaveToken"
    }
    $sec = Import-Clixml -Path $TokenFile
    return [System.Net.NetworkCredential]::new('', $sec).Password
}

if ($SaveToken) { Save-Token; return }

$token = Get-Token

# optional: set project environment once (needs Write projects scope)
if ($SetEnvironment) {
    $body = @{ client_side = $ClientSide; server_side = $ServerSide } | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri "https://api.modrinth.com/v2/project/$ProjectSlug" -Method Patch `
            -Headers @{ Authorization = $token; "User-Agent" = $UserAgent } `
            -ContentType "application/json" -Body $body
        Write-Host "Environment set: client=$ClientSide, server=$ServerSide" -ForegroundColor Green
    } catch {
        Write-Host "Failed to set environment: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message -ForegroundColor Red }
    }
    return
}

# read version + name from gradle.properties
$modVersion = (Select-String -Path "$RepoRoot\gradle.properties" -Pattern '^mod_version=(.+)$').Matches[0].Groups[1].Value.Trim()
$modName    = (Select-String -Path "$RepoRoot\gradle.properties" -Pattern '^mod_name=(.+)$').Matches[0].Groups[1].Value.Trim()

# find the jar
$jar = Get-ChildItem "$RepoRoot\build\libs\*.jar" | Where-Object { $_.Name -notmatch 'sources|javadoc' } | Select-Object -First 1
if (-not $jar) { throw "No jar in $RepoRoot\build\libs - run .\gradlew.bat build first." }

# changelog section for this version
$changelog = "See https://github.com/ErikEdits/JustQuests/blob/main/CHANGELOG.md"
$clPath = "$RepoRoot\CHANGELOG.md"
if (Test-Path $clPath) {
    $lines = Get-Content $clPath
    $start = ($lines | Select-String -Pattern "^## \[$([regex]::Escape($modVersion))\]").LineNumber
    if ($start) {
        $rest = $lines[$start..($lines.Count - 1)]
        $end = ($rest | Select-String -Pattern "^## \[").LineNumber | Select-Object -First 1
        if ($end) { $section = $rest[0..($end - 2)] } else { $section = $rest }
        $changelog = ($section -join "`n").Trim()
    }
}

# pre-flight: project reachable?
try {
    $proj = Invoke-RestMethod -Uri "https://api.modrinth.com/v2/project/$ProjectSlug" -Method Get `
        -Headers @{ Authorization = $token; "User-Agent" = $UserAgent }
    Write-Host "Project: $($proj.title) (id $($proj.id))" -ForegroundColor DarkGray
} catch {
    Write-Host "WARNING: project '$ProjectSlug' not reachable with this token." -ForegroundColor Yellow
}

Write-Host "Uploading $modName $modVersion ($VersionType) - MC $($GameVersions -join ', '); loaders $($Loaders -join ', ')" -ForegroundColor Cyan

$meta = @{
    name           = "$modName $modVersion"
    version_number = $modVersion
    changelog      = $changelog
    dependencies   = @()
    game_versions  = $GameVersions
    version_type   = $VersionType
    loaders        = $Loaders
    featured       = $true
    project_id     = $ProjectSlug
    file_parts     = @("file")
    primary_file   = "file"
} | ConvertTo-Json -Depth 6

# robust multipart upload via HttpClient (PS 5.1 + 7)
Add-Type -AssemblyName System.Net.Http
$client = [System.Net.Http.HttpClient]::new()
[void]$client.DefaultRequestHeaders.TryAddWithoutValidation("Authorization", $token)
[void]$client.DefaultRequestHeaders.TryAddWithoutValidation("User-Agent", $UserAgent)

$form = [System.Net.Http.MultipartFormDataContent]::new()
$jsonContent = [System.Net.Http.StringContent]::new($meta, [System.Text.Encoding]::UTF8, "application/json")
$form.Add($jsonContent, "data")

$fileStream  = [System.IO.File]::OpenRead($jar.FullName)
$fileContent = [System.Net.Http.StreamContent]::new($fileStream)
$fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::new("application/java-archive")
$form.Add($fileContent, "file", $jar.Name)

try {
    $response = $client.PostAsync("https://api.modrinth.com/v2/version", $form).Result
    $respBody = $response.Content.ReadAsStringAsync().Result
    if ($response.IsSuccessStatusCode) {
        $v = $respBody | ConvertFrom-Json
        Write-Host ""
        Write-Host "Uploaded! https://modrinth.com/project/$ProjectSlug/version/$($v.version_number)" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "Upload failed: HTTP $([int]$response.StatusCode) $($response.StatusCode)" -ForegroundColor Red
        Write-Host $respBody -ForegroundColor Red
        exit 1
    }
} finally {
    $fileStream.Dispose(); $form.Dispose(); $client.Dispose()
}
