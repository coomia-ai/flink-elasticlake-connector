param(
  [Parameter(Mandatory)][string]$GradleExecutable,
  [Parameter(Mandatory)][string]$Jdk17Path,
  [Parameter(Mandatory)][string]$SdkRepository,
  [Parameter(Mandatory)][string]$SdkManifest,
  [Parameter(Mandatory)][string]$SdkManifestSha256,
  [Parameter(Mandatory)][string]$RulesRepository,
  [Parameter(Mandatory)][string]$RulesManifest,
  [Parameter(Mandatory)][string]$RulesManifestSha256,
  [Parameter(Mandatory)][string]$RunId,
  [string]$PluginRepository = 'https://plugins.gradle.org/m2',
  [string]$MavenRepository = 'https://repo.maven.apache.org/maven2'
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
if ($RunId -notmatch '^[A-Za-z0-9][A-Za-z0-9._-]*$') { throw 'Invalid source control ID' }
$connectorRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$targetRoot = Join-Path $connectorRoot "build/api-source-controls/$RunId"
if (Test-Path -LiteralPath $targetRoot) { throw 'Source controls exist; never overwrite' }
New-Item -ItemType Directory -Path $targetRoot | Out-Null
$common = @('-PconnectorApiOnly=true', "-Porg.gradle.java.installations.paths=$Jdk17Path",
  "-PsdkRepository=$((Resolve-Path -LiteralPath $SdkRepository).Path)",
  "-PsdkManifest=$((Resolve-Path -LiteralPath $SdkManifest).Path)", "-PsdkManifestSha256=$SdkManifestSha256",
  "-PrulesRepository=$((Resolve-Path -LiteralPath $RulesRepository).Path)",
  "-PrulesManifest=$((Resolve-Path -LiteralPath $RulesManifest).Path)", "-PrulesManifestSha256=$RulesManifestSha256",
  "-PpluginRepository=$PluginRepository", "-PmavenRepository=$MavenRepository", '--offline', '--no-daemon')
$results = @()
foreach ($case in @(
  @{name='wrapper-drift'; message='API source/dependency/pin drift after capture'},
  @{name='outside-source'; message='API compiler/init input outside repository'},
  @{name='baseline-mismatch'; message='API dependency differs from source-bound byte baseline'}
)) {
  $caseRoot = Join-Path $targetRoot $case.name
  $projectRoot = Join-Path $caseRoot 'project'
  New-Item -ItemType Directory -Path $projectRoot -Force | Out-Null
  $files = & git -C $connectorRoot ls-files --cached --others --exclude-standard
  if ($LASTEXITCODE -ne 0) { throw 'Cannot enumerate maintained private inputs' }
  foreach ($relative in $files) {
    $source = Join-Path $connectorRoot $relative
    if (!(Test-Path -LiteralPath $source -PathType Leaf)) { continue }
    $copy = Join-Path $projectRoot $relative
    New-Item -ItemType Directory -Path (Split-Path -Parent $copy) -Force | Out-Null
    Copy-Item -LiteralPath $source -Destination $copy
  }
  $init = Join-Path $projectRoot ("api-publication/negative-controls/"+$case.name+'.gradle')
  $log = Join-Path $caseRoot 'gradle.log'
  $arguments = $common + @('-p', $projectRoot, '-I', $init, '-PapiCandidateId=negative01', ':flink-connector:compileConnectorApiJava', '--rerun-tasks')
  & $GradleExecutable @arguments *> $log
  $code = $LASTEXITCODE
  if ($code -eq 0 -or !(Select-String -LiteralPath $log -SimpleMatch $case.message -Quiet)) { throw ('Unexpected source control result: '+$case.name) }
  $results += @{name=$case.name; rejected=$true; exitCode=$code; expected=$case.message; logSha256=(Get-FileHash -LiteralPath $log -Algorithm SHA256).Hash.ToLowerInvariant()}
}
$results | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $targetRoot 'results.json') -Encoding utf8
$results | ConvertTo-Json -Depth 10
