# Publishing scripts

## publish-modrinth-desktop.ps1 (recommended)

Convenience version that **stores your token encrypted** (Windows DPAPI,
per-user) so you don't paste it each time. A copy lives on the Desktop as
`justquests-publish.ps1`.

```powershell
# one-time: save token (encrypted to %USERPROFILE%\.justquests\)
.\justquests-publish.ps1 -SaveToken
# one-time (optional): set project environment client/server
.\justquests-publish.ps1 -SetEnvironment
# each release: build then upload
.\gradlew.bat build
.\justquests-publish.ps1                 # release
.\justquests-publish.ps1 -VersionType beta
```

Token scopes needed: "Create versions" + "Read projects" (plus "Write
projects" only for `-SetEnvironment`). Edit `$GameVersions`, `$Loaders`,
`$ClientSide`, `$ServerSide` at the top when they change.
Note: Modrinth environment (client/server) is a **project** setting, not
per-version — that's why it's a separate `-SetEnvironment` step.

---


## publish-modrinth.ps1

Uploads the built jar to Modrinth as a new version. You only supply your token.

### One-time setup
1. Create the project on https://modrinth.com/ with slug **`justquests`**
   and **publish it** (uploading by slug needs a real, non-draft project).
   Set the environment (client/server), description and icon
   (`docs/assets/icon-512.png`) on the project page.
2. Generate a Personal Access Token at
   https://modrinth.com/settings/pats with scopes
   **"Create versions"** and **"Read projects"**.

The script does a pre-flight check and prints Modrinth's exact error
message if the upload is rejected, so any problem is easy to diagnose.

### Each release
```powershell
# 1. build the jar
.\gradlew.bat build

# 2. upload (default version type = release)
.\scripts\publish-modrinth.ps1 -Token "mrp_yourtokenhere"

# or as beta / alpha
.\scripts\publish-modrinth.ps1 -Token "mrp_yourtokenhere" -VersionType beta
```

The script reads the version from `gradle.properties`, finds the jar in
`build/libs`, pulls the matching `CHANGELOG.md` section as the version
changelog, and tags it with MC `1.21.1` + loader `neoforge`.

To add more MC versions or loaders later, edit the `$GameVersions` /
`$Loaders` arrays near the top of the script.

> Your token is never stored or committed — you pass it on the command
> line each time.
