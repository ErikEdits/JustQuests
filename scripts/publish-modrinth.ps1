<#
.SYNOPSIS
    Uploads the built JustQuests jar to Modrinth.

.DESCRIPTION
    Reads the version from gradle.properties, finds the jar in build/libs,
    pulls the matching changelog section, and uploads it as a new version
    to the Modrinth project. You only provide your Modrinth token.

    ONE-TIME SETUP (do this once on the website before first run):
      1. Create the project at https://modrinth.com/  (slug: justquests)
         and PUBLISH it (a Draft is not a valid upload target by slug).
      2. On the project page set the Environment (e.g. client/server)
         and description/icon as you like.
      3. Create a Personal Access Token at
         https://modrinth.com/settings/pats  with the scope
         "Create versions" (and "Read projects").

.EXAMPLE
    ./scripts/publish-modrinth.ps1 -Token "mrp_xxx"
    ./scripts/publish-modrinth.ps1 -Token "mrp_xxx" -VersionType beta
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$Token,

    [ValidateSet("release", "beta", "alpha")]
    [string]$VersionType = "release",

    [string]$ProjectSlug = "justquests"
)

$ErrorActionPreference = "Stop"

# --- editable metadata -------------------------------------------------
$GameVersions = @("1.21.1")     # add more MC versions here when supported
$Loaders      = @("neoforge")   # this jar is NeoForge; add "fabric"/"forge" after porting
$UserAgent    = "ErikEdits/JustQuests/publish-script"
# -----------------------------------------------------------------------

$repoRoot = Split-Path $PSScriptRoot -Parent

# version + name from gradle.properties
$modVersion = (Select-String -Path "$repoRoot\gradle.properties" -Pattern '^mod_version=(.+)$').Matches[0].Groups[1].Value.Trim()
$modName    = (Select-String -Path "$repoRoot\gradle.properties" -Pattern '^mod_name=(.+)$').Matches[0].Groups[1].Value.Trim()

# locate the jar
$jar = Get-ChildItem "$repoRoot\build\libs\*.jar" | Where-Object { $_.Name -notmatch 'sources|javadoc' } | Select-Object -First 1
if (-not $jar) { throw "No jar found in build/libs. Run .\gradlew.bat build first." }

# changelog: grab the section for this version from CHANGELOG.md
$changelog = "See https://github.com/ErikEdits/JustQuests/blob/main/CHANGELOG.md"
$clPath = "$repoRoot\CHANGELOG.md"
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

# pre-flight: does the project exist and is it visible to this token?
try {
    $proj = Invoke-RestMethod -Uri "https://api.modrinth.com/v2/project/$ProjectSlug" -Method Get `
        -Headers @{ Authorization = $Token; "User-Agent" = $UserAgent }
    Write-Host "Project found: $($proj.title) (id $($proj.id))" -ForegroundColor DarkGray
}
catch {
    Write-Host "WARNING: project '$ProjectSlug' not reachable with this token." -ForegroundColor Yellow
    Write-Host "Make sure you created AND published it on modrinth.com first," -ForegroundColor Yellow
    Write-Host "and that the token has the 'Read projects' + 'Create versions' scopes." -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "Project : $ProjectSlug"        -ForegroundColor Cyan
Write-Host "Version : $modVersion ($VersionType)" -ForegroundColor Cyan
Write-Host "Jar     : $($jar.Name)"          -ForegroundColor Cyan
Write-Host "MC      : $($GameVersions -join ', ')  Loaders: $($Loaders -join ', ')" -ForegroundColor Cyan

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

# --- robust multipart upload via HttpClient (works on PS 5.1 and 7) ----
Add-Type -AssemblyName System.Net.Http

$client = [System.Net.Http.HttpClient]::new()
# TryAddWithoutValidation: .NET's strict header parser rejects a User-Agent
# with multiple slashes (product/product/version), so bypass validation.
[void]$client.DefaultRequestHeaders.TryAddWithoutValidation("Authorization", $Token)
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
    }
    else {
        Write-Host ""
        Write-Host "Upload failed: HTTP $([int]$response.StatusCode) $($response.StatusCode)" -ForegroundColor Red
        Write-Host "Modrinth says:" -ForegroundColor Red
        Write-Host $respBody -ForegroundColor Red
        exit 1
    }
}
finally {
    $fileStream.Dispose()
    $form.Dispose()
    $client.Dispose()
}
