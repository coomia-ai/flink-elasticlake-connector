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
if ($RunId -notmatch '^[A-Za-z0-9][A-Za-z0-9._-]*$') { throw 'Invalid engineering control RunId' }
$connectorRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$controlRoot = Join-Path $connectorRoot "build/foundation-engineering-controls/$RunId"
if (Test-Path -LiteralPath $controlRoot) { throw 'Engineering control directory exists; never overwrite' }
$sdkRepoPath = (Resolve-Path -LiteralPath $SdkRepository).Path
$sdkManifestPath = (Resolve-Path -LiteralPath $SdkManifest).Path
$rulesRepoPath = (Resolve-Path -LiteralPath $RulesRepository).Path
$rulesManifestPath = (Resolve-Path -LiteralPath $RulesManifest).Path
if ((Get-FileHash -LiteralPath $sdkManifestPath -Algorithm SHA256).Hash.ToLowerInvariant() -ne $SdkManifestSha256) { throw 'Trusted SDK manifest differs' }
if ((Get-FileHash -LiteralPath $rulesManifestPath -Algorithm SHA256).Hash.ToLowerInvariant() -ne $RulesManifestSha256) { throw 'Trusted rules manifest differs' }
New-Item -ItemType Directory -Path $controlRoot | Out-Null
$sdk = Get-Content -LiteralPath $sdkManifestPath -Raw | ConvertFrom-Json
$sdkJars = @($sdk.files | Where-Object { $_.path -match '^io/elasticlake/elasticlake-client-api/[^/]+/[^/]+\.jar$' })
$sdkPoms = @($sdk.files | Where-Object { $_.path -match '^io/elasticlake/elasticlake-client-api/[^/]+/[^/]+\.pom$' })
if ($sdkJars.Count -ne 1 -or $sdkPoms.Count -ne 1 -or $sdk.version -ne '0.1.0-SNAPSHOT') { throw 'Expected one exact SDK API artifact/POM' }
$sdkJar = Join-Path $sdkRepoPath $sdkJars[0].path
if ((Get-FileHash -LiteralPath $sdkJar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $sdkJars[0].sha256) { throw 'Original SDK JAR differs' }
$privateM2 = Join-Path $controlRoot 'private-m2'
$localVersion = Join-Path $privateM2 'io/elasticlake/elasticlake-client-api/0.1.0-SNAPSHOT'
New-Item -ItemType Directory -Path $localVersion -Force | Out-Null
Copy-Item -LiteralPath $sdkJar -Destination (Join-Path $localVersion 'elasticlake-client-api-0.1.0-SNAPSHOT.jar')
Copy-Item -LiteralPath (Join-Path $sdkRepoPath $sdkPoms[0].path) -Destination (Join-Path $localVersion 'elasticlake-client-api-0.1.0-SNAPSHOT.pom')
$positiveLog = Join-Path $controlRoot 'private-maven-local-availability.log'
$availabilityRoot = Join-Path $controlRoot 'availability-consumer'
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'maven-local-availability') -Destination $availabilityRoot -Recurse
$positiveArgs = @('-p',$availabilityRoot,"-Dmaven.repo.local=$privateM2",("-PexpectedSdkJarSha256=" + $sdkJars[0].sha256),'verifyAvailability','--offline','--no-daemon')
& $GradleExecutable @positiveArgs *> $positiveLog
if ($LASTEXITCODE -ne 0 -or -not (Select-String -LiteralPath $positiveLog -SimpleMatch 'PRIVATE_MAVEN_LOCAL_AVAILABLE' -Quiet)) { throw "Private mavenLocal availability control failed: $positiveLog" }
$brokenSdk = Join-Path $controlRoot 'broken-sdk-repository'
Copy-Item -LiteralPath $sdkRepoPath -Destination $brokenSdk -Recurse
$privateJar = [System.IO.Path]::GetFullPath((Join-Path $brokenSdk $sdkJars[0].path))
if (-not $privateJar.StartsWith(([System.IO.Path]::GetFullPath($controlRoot) + [System.IO.Path]::DirectorySeparatorChar), [System.StringComparison]::OrdinalIgnoreCase)) { throw 'Control refuses deletion outside its private directory' }
Remove-Item -LiteralPath $privateJar
$common = @('-PnonProductProbe=true',"-Porg.gradle.java.installations.paths=$Jdk17Path", "-PsdkManifest=$sdkManifestPath", "-PsdkManifestSha256=$SdkManifestSha256", "-PrulesRepository=$rulesRepoPath", "-PrulesManifest=$rulesManifestPath", "-PrulesManifestSha256=$RulesManifestSha256", "-PpluginRepository=$PluginRepository", "-PmavenRepository=$MavenRepository",'--offline','--no-daemon')
$cases = @(
  @{name='missing-sdk-repository'; repository=(Join-Path $controlRoot 'missing-sdk-repository'); target=':flink-connector:compileConnectorApiJava'; init=$null; expected=@('Missing candidate repository/manifest')},
  @{name='maven-local-fallback'; repository=$brokenSdk; target=':flink-connector:compileConnectorApiJava'; init='maven-local-fallback.gradle'; expected=@('Candidate inventory differs: missing=')},
  @{name='rules-in-ordinary-tests'; repository=$sdkRepoPath; target=':flink-connector:compileJava'; init='rules-in-ordinary-tests.gradle'; expected=@('Architecture artifact leaked into','ArchUnit leaked into ordinary/production')},
  @{name='rules-in-product-jar'; repository=$sdkRepoPath; target=':flink-connector:verifyConnectorPackaging'; init='rules-in-product-jar.gradle'; expected=@('JAR classes differ from own source outputs')}
)
$results = @()
Push-Location $connectorRoot
try {
  foreach ($case in $cases) {
    $log = Join-Path $controlRoot ($case.name + '.log')
    $buildArguments = $common + @("-PsdkRepository=$($case.repository)", "-PfoundationRunId=$RunId-$($case.name)", "-Dmaven.repo.local=$privateM2", $case.target,'--rerun-tasks')
    if ($case.init) { $buildArguments += @('-I',(Join-Path $PSScriptRoot $case.init)) }
    & $GradleExecutable @buildArguments *> $log
    $exitCode = $LASTEXITCODE
    $matched = @($case.expected | Where-Object { Select-String -LiteralPath $log -SimpleMatch $_ -Quiet })
    $rejected = $exitCode -ne 0 -and $matched.Count -gt 0
    if ($case.name -eq 'maven-local-fallback' -and -not (Select-String -LiteralPath $log -SimpleMatch 'CONTROL_PRIVATE_MAVEN_LOCAL_FALLBACK_ATTEMPTED' -Quiet)) { $rejected = $false }
    $results += [ordered]@{name=$case.name;rejected=$rejected;exitCode=$exitCode;matched=$matched;logSha256=(Get-FileHash -LiteralPath $log -Algorithm SHA256).Hash.ToLowerInvariant()}
    [ordered]@{scope='non-product-engineering-controls';privateMavenLocalAvailable=$true;availabilityLogSha256=(Get-FileHash -LiteralPath $positiveLog -Algorithm SHA256).Hash.ToLowerInvariant();cases=$results;productAccepted=$false} | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $controlRoot 'results.json') -Encoding utf8
    if (-not $rejected) { Get-Content -LiteralPath $log -Tail 45; throw "Expected rejection not observed: $($case.name)" }
    Write-Output "ENGINEERING_CONTROL_REJECTED $($case.name)"
  }
} finally {
  Pop-Location
}
