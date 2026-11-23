param([switch]$Build)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$configFile=Join-Path $projectRoot '.env'
if(!(Test-Path -LiteralPath $configFile)){throw 'Create your ignored local .env using .env.example first.'}
foreach($line in [IO.File]::ReadAllLines($configFile)){if($line -match '^([A-Z][A-Z0-9_]*)=(.*)$'){[Environment]::SetEnvironmentVariable($matches[1],$matches[2],'Process')}}
$jar=Join-Path $projectRoot 'backend/target-modern/permit-engine1-1.0.0.jar'
Push-Location (Join-Path $projectRoot 'backend')
try{if($Build -or !(Test-Path -LiteralPath $jar)){& mvn -B '-Dpermit.build.directory=target-modern' '-DskipTests' package;if($LASTEXITCODE -ne 0){throw 'Backend build failed.'}};$javaExecutable=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}else{(Get-Command java -ErrorAction Stop).Source};& $javaExecutable -Xmx512m -jar $jar}finally{Pop-Location}
