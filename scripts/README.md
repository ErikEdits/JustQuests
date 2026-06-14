# Publishing scripts

## publish-modrinth.ps1

Uploads the built jar to Modrinth as a new version. You only supply your token.

### One-time setup
1. Create the project on https://modrinth.com/ with slug **`justquests`**.
   Set the environment (client/server), description and icon
   (`docs/assets/icon-512.png`) on the project page.
2. Generate a Personal Access Token at
   https://modrinth.com/settings/pats with scope **"Create versions"**.

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
