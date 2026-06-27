param(
    [string]$Profile = "local",
    [string]$EnvFile = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDir = Resolve-Path (Join-Path $scriptDir "..")

if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path $scriptDir "onebot-local.env"
}

if (-not (Test-Path -LiteralPath $EnvFile)) {
    Write-Host "Missing env file: $EnvFile" -ForegroundColor Yellow
    Write-Host "Copy scripts\onebot-local.env.example to scripts\onebot-local.env, then fill SnowLuma token and Dify API keys."
    exit 1
}

# Get-Content -LiteralPath $EnvFile | ForEach-Object {
Get-Content -LiteralPath $EnvFile -Encoding UTF8 | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
        return
    }

    $separatorIndex = $line.IndexOf("=")
    if ($separatorIndex -le 0) {
        Write-Host "Skip invalid env line: $line" -ForegroundColor Yellow
        return
    }

    $key = $line.Substring(0, $separatorIndex).Trim()
    $value = $line.Substring($separatorIndex + 1).Trim()
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    [Environment]::SetEnvironmentVariable($key, $value, "Process")
}

Write-Host "Starting QQ bot local OneBot integration..."
Write-Host "profile=$Profile"
Write-Host "onebot.ws.enabled=$env:QQBOT_ONEBOT_WS_ENABLED"
Write-Host "onebot.ws.url=$env:QQBOT_ONEBOT_WS_URL"
Write-Host "onebot.self-id=$env:QQBOT_ONEBOT_SELF_ID"
Write-Host "onebot.allowed-group-ids=$env:QQBOT_ONEBOT_ALLOWED_GROUP_IDS"
Write-Host "meme.base-dir=$env:QQBOT_MEME_BASE_DIR"
Write-Host "dify.enabled=$env:DIFY_ENABLED"
Write-Host "bot.display-name=$env:QQBOT_DISPLAY_NAME"
Write-Host "passive.trigger-words=$env:QQBOT_PASSIVE_CHAT_TRIGGER_WORDS"
Write-Host "dify.workflow.meme-scene=$env:DIFY_MEME_SCENE_WORKFLOW"
Write-Host "dify.workflow.passive-chat=$env:DIFY_PASSIVE_CHAT_WORKFLOW"
Write-Host "dify.workflow.active-chat=$env:DIFY_ACTIVE_CHAT_WORKFLOW"
Write-Host "token/api-key values are intentionally hidden."

Push-Location $projectDir
try {
    & .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=$Profile"
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
