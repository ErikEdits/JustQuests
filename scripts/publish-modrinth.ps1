<#
.SYNOPSIS
    Uploads the built JustQuests jar to Modrinth.

.DESCRIPTION
    Reads the version from gradle.properties, finds the jar in build/libs,
    pulls the matching changelog section, and uploads it as a new version
    to the Modrinth project. You only provide your Modrinth token.

    ONE-TIME SETUP (do this once on the website before first run):
      1. Create the project at https://modrinth.com/  (slug: justquests)
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
# -----------------------------------------------------------------------

$repoRoot = Split-Path $PSScriptRoot -Parent

# version from gradle.properties
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
} | ConvertTo-Json -Depth 6 -Compress

# multipart/form-data: json "data" part + the jar file part
$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"
$fileBytes = [System.IO.File]::ReadAllBytes($jar.FullName)
$enc = [System.Text.Encoding]::GetEncoding("iso-8859-1")
$fileContent = $enc.GetString($fileBytes)

$body = (
    "--$boundary", 'Content-Disposition: form-data; name="data"', 'Content-Type: application/json', '', $meta,
    "--$boundary", "Content-Disposition: form-data; name=`"file`"; filename=`"$($jar.Name)`"", 'Content-Type: application/java-archive', '', $fileContent,
    "--$boundary--", ''
) -join $LF

try {
    $resp = Invoke-RestMethod -Uri "https://api.modrinth.com/v2/version" -Method Post `
        -Headers @{ Authorization = $Token } `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -Body ([System.Text.Encoding]::GetEncoding("iso-8859-1").GetBytes($body))
    Write-Host ""
    Write-Host "Uploaded! https://modrinth.com/project/$ProjectSlug/version/$($resp.version_number)" -ForegroundColor Green
}
catch {
    Write-Host ""
    Write-Host "Upload failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message -ForegroundColor Red }
    exit 1
}
